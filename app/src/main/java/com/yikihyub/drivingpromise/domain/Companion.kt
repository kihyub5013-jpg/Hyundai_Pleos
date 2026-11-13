package com.yikihyub.drivingpromise.domain

import android.util.Log
import ai.pleos.playground.navi.constants.RouteOption
import com.yikihyub.drivingpromise.data.Store
import com.yikihyub.drivingpromise.vehicle.LocationGateway
import com.yikihyub.drivingpromise.vehicle.NaviEvent
import com.yikihyub.drivingpromise.vehicle.NaviGateway
import com.yikihyub.drivingpromise.vehicle.NaviPlace
import com.yikihyub.drivingpromise.vehicle.SpeechGateway
import com.yikihyub.drivingpromise.vehicle.SpeedGateway
import com.yikihyub.drivingpromise.vehicle.VehicleGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** 지금 화면에 크게 띄울 페달 안내 */
data class ActiveAlert(val alert: PedalAlert, val atMs: Long)

/** 출발 전 브리핑 결과 (화면 카드용) */
data class Briefing(
    val car: CarBrief,
    val sunset: ZonedDateTime?,
    val sunsetVerdict: SunsetVerdict,
    val routePreview: String?,
    val atMs: Long,
)

/** 진행 중인 운행 */
data class LiveTrip(
    val startMs: Long,
    val startOdoKm: Float?,
    val night: Boolean = false,
    val rain: Boolean = false,
    val highway: Boolean = false,
    val longestStintMin: Int = 0,
    val companion: Boolean? = null,
    val pedalAlerts: Int = 0,
    val destination: String = "",
)

/** 시연 시나리오. Virtual Console 없이도 PT 영상에서 같은 흐름을 보여준다. */
enum class DemoScenario(val title: String) {
    PARKING_PEDAL("주차장 후진 중 페달 혼동"),
    BRAKE_WEAK("브레이크를 덜 밟음"),
    LAUNCH("정차 후 급출발"),
}

/**
 * 운전 동반자 엔진. 앱 생명주기 동안 하나. 차량·내비·위치 신호를 받아
 * 페달 확인 안내, 약속 확인, 운전 중 안내를 음성으로 내보내고 운행 기록을 남긴다.
 *
 * 음성 우선순위: 페달 안내는 지금 말하던 문장을 끊고 즉시. 나머지는 순서대로 하나씩.
 */
class DrivingCompanion(
    private val scope: CoroutineScope,
    private val store: Store,
    private val vehicle: VehicleGateway,
    private val speed: SpeedGateway,
    private val location: LocationGateway,
    private val navi: NaviGateway,
    private val speech: SpeechGateway,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val tag = "Companion"
    private val guard = PedalGuard()
    private val coach = DriveCoach()

    private val _alert = MutableStateFlow<ActiveAlert?>(null)
    val alert: StateFlow<ActiveAlert?> = _alert.asStateFlow()

    private val _lastSaid = MutableStateFlow<String?>(null)
    val lastSaid: StateFlow<String?> = _lastSaid.asStateFlow()

    private val _briefing = MutableStateFlow<Briefing?>(null)
    val briefing: StateFlow<Briefing?> = _briefing.asStateFlow()

    private val _trip = MutableStateFlow<LiveTrip?>(null)
    val trip: StateFlow<LiveTrip?> = _trip.asStateFlow()

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _sensitive = MutableStateFlow(false)
    val sensitive: StateFlow<Boolean> = _sensitive.asStateFlow()

    private val _parkedAt = MutableStateFlow<String?>(null)
    val parkedAt: StateFlow<String?> = _parkedAt.asStateFlow()

    private val _demo = MutableStateFlow<DemoScenario?>(null)
    val demo: StateFlow<DemoScenario?> = _demo.asStateFlow()

    /** 시연 중에는 차량 신호 대신 이 값을 쓴다 */
    private var demoSpeed: Float? = null
    private var demoBrake: Boolean? = null
    private var demoGear: Gear? = null
    private var demoNearParking = false

    private val queue = Channel<String>(capacity = 16)
    private var speaking: Job? = null
    private var urgent: Job? = null
    private val jobs = mutableListOf<Job>()
    private var arrivedAtMs: Long? = null
    private var lastAsked: Long = 0

    fun start() {
        jobs += scope.launch { speakLoop() }
        jobs += scope.launch { pedalLoop() }
        jobs += scope.launch { coachLoop() }
        jobs += scope.launch { store.settings.collect { guard.sensitivity = it.sensitivity } }
        jobs += scope.launch { speed.speed.filterNotNull().collect { if (demoSpeed == null) _speedKmh.value = it.kmh } }
        jobs += scope.launch { vehicle.gear.collect { onGear(demoGear ?: it) } }
        jobs += scope.launch { vehicle.signal.collect { coach.onSignal(it, System.currentTimeMillis()) } }
        jobs += scope.launch { vehicle.steeringDeg.collect { coach.onSteering(it) } }
        jobs += scope.launch {
            vehicle.wiperOn.filterNotNull().distinctUntilChanged().collect { on ->
                if (on) _trip.value?.let { _trip.value = it.copy(rain = true) }
                coach.onWiper(on, vehicle.lowBeamOn.value)?.let(::say)
                if (on) DepartureCheck.rain(store.settings.value.promises, true, navi.destination.value?.remainDistanceM?.div(1000.0))?.let(::say)
            }
        }
        jobs += scope.launch {
            vehicle.drivingMinutes.filterNotNull().collect { min ->
                _trip.value?.let { if (min > it.longestStintMin) _trip.value = it.copy(longestStintMin = min) }
                coach.onDrivingMinutes(min, store.settings.value.promises)?.let(::say)
            }
        }
        jobs += scope.launch {
            navi.turns.collect { turns ->
                val next = turns.firstOrNull() ?: return@collect
                if (next.turn == Turn.HIGHWAY_IN && next.distanceM < 100) _trip.value?.let { _trip.value = it.copy(highway = true) }
                coach.onTurn(next, _speedKmh.value)?.let(::say)
            }
        }
        jobs += scope.launch {
            navi.cautions.collect { list -> list.forEach { c -> coach.onCaution(c.caution, c.remainM, c.id)?.let(::say) } }
        }
        jobs += scope.launch {
            navi.destination.collect { d ->
                if (d == null) return@collect
                coach.familiarRoute = store.visitCount(d.name) > 0
                coach.onRemainDistance(d.remainDistanceM)?.let(::say)
                _trip.value?.let { if (it.destination != d.name) _trip.value = it.copy(destination = d.name) }
            }
        }
        jobs += scope.launch { navi.events.collect(::onNaviEvent) }
        jobs += scope.launch { store.spots.collect { watchSpots(it) } }
        location.onSpeed = { kmh, t -> speed.onLocationSpeed(kmh, t) }
    }

    fun stop() { jobs.forEach { it.cancel() }; jobs.clear(); location.onSpeed = null }

    // ───────────── 음성 ─────────────

    /** 순서대로 읽을 안내 */
    fun say(text: String) {
        if (text.isBlank()) return
        Log.i(tag, "say: $text")
        queue.trySend(text)
    }

    /** 지금 말하던 것을 끊고 바로 */
    private fun sayNow(text: String) {
        speaking?.cancel()
        urgent?.cancel()
        _lastSaid.value = text
        urgent = scope.launch { speech.say(text) }
    }

    private suspend fun speakLoop() {
        for (text in queue) {
            urgent?.join()
            _lastSaid.value = text
            val j = scope.launch { speech.say(text) }
            speaking = j
            j.join()
        }
    }

    // ───────────── 페달 확인 안내 ─────────────

    private suspend fun pedalLoop() {
        while (scope.isActive) {
            delay(100)
            if (!store.settings.value.pedalGuard) continue
            val now = System.currentTimeMillis()
            val gear = demoGear ?: vehicle.gear.value
            if (demoGear == null && gear == Gear.UNKNOWN && !vehicle.connected.value && demoSpeed == null) continue
            val nearParking = demoNearParking || coach.nearParking(navi.destination.value?.remainDistanceM) ||
                (arrivedAtMs?.let { now - it < 5 * 60_000 } ?: false)
            val sample = PedalSample(now, demoSpeed ?: _speedKmh.value, demoBrake ?: vehicle.brake.value, gear, nearParking)
            if (demoSpeed != null) _speedKmh.value = demoSpeed!!
            _sensitive.value = guard.isSensitive(sample)
            val a = guard.onSample(sample)
            if (a != null) onPedalAlert(a)
            else if (guard.hardStop) saveSpot("급하게 멈춘 곳")
        }
    }

    private fun onPedalAlert(a: PedalAlert) {
        Log.i(tag, "PEDAL $a speed=${_speedKmh.value} accel=${guard.lastAccel}")
        _alert.value = ActiveAlert(a, System.currentTimeMillis())
        sayNow(a.speech)
        _trip.value?.let { _trip.value = it.copy(pedalAlerts = it.pedalAlerts + 1) }
        saveSpot("페달 확인 안내가 울린 곳")
        scope.launch { delay(4_000); if (_alert.value?.alert == a) _alert.value = null }
    }

    fun dismissAlert() { _alert.value = null }

    private fun saveSpot(reason: String) {
        if (_demo.value != null) return // 시연 값으로 지도를 더럽히지 않는다
        val loc = location.last.value ?: return
        val spot = SurpriseSpot(loc.latitude, loc.longitude, System.currentTimeMillis(), reason, navi.currentAddress.value.orEmpty())
        if (store.addSpot(spot)) Log.i(tag, "surprise spot saved: $spot")
    }

    private var watched = 0
    private fun watchSpots(spots: List<SurpriseSpot>) {
        if (!location.isStarted || spots.size == watched) return
        location.unwatchAll()
        spots.forEach { s -> location.watch(s) { say("지난번 놀랐던 곳이에요. 천천히 가요.") } }
        watched = spots.size
    }

    /** 위치 권한을 받은 뒤 다시 등록 */
    fun onLocationReady() { watched = 0; watchSpots(store.spots.value) }

    // ───────────── 운전 중 안내 ─────────────

    private suspend fun coachLoop() {
        var tick = 0
        while (scope.isActive) {
            delay(1_000)
            tick++
            val now = System.currentTimeMillis()
            val v = _speedKmh.value
            coach.checkSignalLeftOn(now, v)?.let(::say)
            val dark = isDark()
            coach.checkHeadlight(dark, vehicle.lowBeamOn.value, v, now)?.let(::say)
            _trip.value?.let { if (dark && v > 5f && !it.night) _trip.value = it.copy(night = true) }
            if (_trip.value != null && tick % 2 == 0) navi.poll()
        }
    }

    private fun onGear(g: Gear) {
        val now = System.currentTimeMillis()
        if (g != Gear.PARK && g != Gear.UNKNOWN && _trip.value == null) {
            _trip.value = LiveTrip(startMs = now, startOdoKm = vehicle.odometerKm.value)
            coach.resetTrip()
            if (isDark() && Promise.NIGHT_WITH_COMPANION in store.settings.value.promises) scope.launch { askCompanion() }
        } else if (g == Gear.PARK) {
            val t = _trip.value ?: return
            _trip.value = null
            if (now - t.startMs < 30_000) return // 잠깐 기어만 바꾼 것
            val km = vehicle.odometerKm.value?.let { end -> t.startOdoKm?.let { (end - it).toDouble() } } ?: 0.0
            store.addTrip(
                Trip(t.startMs, now, km.coerceAtLeast(0.0), t.destination, t.night, t.rain, t.highway,
                    maxOf(t.longestStintMin, ((now - t.startMs) / 60_000).toInt()), t.companion, t.pedalAlerts),
            )
        }
    }

    private suspend fun askCompanion() {
        if (System.currentTimeMillis() - lastAsked < 60_000) return
        lastAsked = System.currentTimeMillis()
        say("밤 운전이에요. 옆에 같이 타신 분 있으세요?")
        delay(3_500)
        val heard = speech.listenOnce() ?: return
        val yes = (VoiceCommand.parse(heard) as? VoiceCommand.Yes)?.yes ?: return
        _trip.value?.let { _trip.value = it.copy(companion = yes) }
        say(if (yes) "좋아요. 천천히 가요." else "밤엔 동승자 있을 때만 운전하기로 약속했어요. 오늘은 꼭 필요한 길인지 생각해 보세요.")
    }

    private fun onNaviEvent(e: NaviEvent) {
        when (e) {
            is NaviEvent.RouteStarted -> scope.launch {
                applyRoutePromise()
                delay(2_500) // 내비가 목적지·TBT·주의구간을 채울 시간
                navi.refresh()
                delay(1_000)
                departureBriefing(spoken = true, includeCar = false)
            }
            is NaviEvent.Arrived -> scope.launch {
                arrivedAtMs = System.currentTimeMillis()
                store.addVisit(e.name)
                val addr = navi.fetchCurrentAddress()
                _parkedAt.value = listOfNotNull(e.name.ifBlank { null }, addr).joinToString(" · ")
                say("도착했어요. 주차장이에요, 천천히 브레이크 확인하세요. 주차 위치를 저장해 둘게요.")
            }
            NaviEvent.RouteCancelled -> Unit
        }
    }

    // ───────────── 출발 전 ─────────────

    fun routeOption(promises: Set<Promise> = store.settings.value.promises): RouteOption = when {
        Promise.AVOID_HIGHWAY in promises -> RouteOption.FREE
        promises.isNotEmpty() -> RouteOption.COMFORTABLE
        else -> RouteOption.RECOMMENDED
    }

    private var lastAppliedOptionMs = 0L
    private var requestedByApp = false

    /** 내비에서 직접 목적지를 정했어도 고속도로 피하기 약속이면 무료도로 우선으로 */
    private fun applyRoutePromise() {
        if (requestedByApp) { requestedByApp = false; return }
        if (Promise.AVOID_HIGHWAY !in store.settings.value.promises) return
        val now = System.currentTimeMillis()
        if (now - lastAppliedOptionMs < 60_000) return
        lastAppliedOptionMs = now
        navi.changeRouteOption(RouteOption.FREE)
        say("고속도로 피하기 약속이 있어서 무료도로 우선으로 바꿀게요.")
    }

    /**
     * 오늘의 차 상태 + 해 지기 전 도착 + 경로 미리보기.
     * 앱을 켤 때(차 상태만)와 경로 안내가 시작될 때(일몰·경로) 부른다.
     */
    suspend fun departureBriefing(spoken: Boolean = true, includeCar: Boolean = true): Briefing {
        val car = CarCheck.brief(vehicle.readCarStatus())
        val now = ZonedDateTime.now(zone)
        val (lat, lng) = latLng()
        val sunset = Sun.sunset(LocalDate.now(zone), lat, lng, zone)
        val dest = navi.destination.value
        val verdict = if (dest != null) DepartureCheck.sunset(now, dest.remainTimeS, sunset) else SunsetVerdict.Unknown
        val preview = if (dest != null && !coach.familiarRoute) RoutePreview.speech(navi.turns.value, navi.cautions.value.map { it.caution }) else null
        val b = Briefing(car, sunset, verdict, preview, System.currentTimeMillis())
        _briefing.value = b
        if (spoken) {
            if (includeCar) say(car.speech)
            if (Promise.NO_NIGHT in store.settings.value.promises && verdict.speech.isNotBlank()) {
                say(verdict.speech + if (verdict is SunsetVerdict.InTime && routeOption() == RouteOption.COMFORTABLE) " 편한 길로 갈게요." else "")
            }
            DepartureCheck.rain(store.settings.value.promises, vehicle.wiperOn.value == true, dest?.remainDistanceM?.div(1000.0))?.let(::say)
            preview?.let(::say)
        }
        return b
    }

    private fun latLng(): Pair<Double, Double> = location.last.value?.let { it.latitude to it.longitude } ?: (37.5665 to 126.9780)

    fun isDark(now: ZonedDateTime = ZonedDateTime.now(zone)): Boolean { val (lat, lng) = latLng(); return Sun.isDark(now, lat, lng) }

    fun todaySunset(): ZonedDateTime? { val (lat, lng) = latLng(); return Sun.sunset(LocalDate.now(zone), lat, lng, zone) }

    // ───────────── 말로 하기 ─────────────

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    fun listenCommand() {
        if (_listening.value) { speech.stopListening(); return }
        scope.launch {
            _listening.value = true
            say("말씀하세요.")
            delay(1_200)
            val heard = speech.listenOnce()
            _listening.value = false
            if (heard.isNullOrBlank()) { say("잘 못 들었어요. 다시 한 번 눌러 주세요."); return@launch }
            handle(VoiceCommand.parse(heard), heard)
        }
    }

    /** 화면 버튼·음성 모두 여기로 */
    fun handle(cmd: VoiceCommand, heard: String = "") {
        when (cmd) {
            VoiceCommand.GoHome -> navi.home.value?.let { go(it) } ?: say("내비에 집이 저장돼 있지 않아요.")
            VoiceCommand.GoOffice -> navi.office.value?.let { go(it) } ?: say("내비에 회사가 저장돼 있지 않아요.")
            is VoiceCommand.GoTo -> {
                val places = listOfNotNull(navi.home.value, navi.office.value) + navi.recent.value
                VoiceCommand.bestMatch(cmd.query, places) { it.name }?.let { go(it) } ?: say("${cmd.query}은 최근 목적지에 없어요. 내비에서 한 번 찾아 주세요.")
            }
            is VoiceCommand.SetPromises -> {
                store.setPromises(store.settings.value.promises + cmd.promises)
                say("약속을 정했어요. ${cmd.promises.joinToString(", ") { it.title }}.")
            }
            VoiceCommand.HowFar -> navi.destination.value?.let {
                val min = (it.remainTimeS + 59) / 60
                say("${it.name}까지 ${min}분 남았어요.")
            } ?: say("지금은 안내 중인 목적지가 없어요.")
            VoiceCommand.CarStatus -> scope.launch { say(CarCheck.brief(vehicle.readCarStatus()).speech) }
            VoiceCommand.MonthReport -> say(monthReport().speech())
            is VoiceCommand.Yes, VoiceCommand.Unknown -> say(if (heard.isNotBlank()) "\"$heard\"는 아직 못 알아들어요. 집에 가자, 얼마나 남았어 처럼 말해 주세요." else "다시 말해 주세요.")
        }
    }

    fun go(p: NaviPlace) {
        val opt = routeOption()
        requestedByApp = true
        if (navi.navigateTo(p, opt)) {
            val how = when (opt) { RouteOption.FREE -> "무료도로 우선으로"; RouteOption.COMFORTABLE -> "편한 길로"; else -> "" }
            say("${p.name}으로 $how 길을 찾을게요.".replace("  ", " "))
        } else {
            requestedByApp = false
            say("내비에 연결되지 않았어요.")
        }
    }

    fun monthReport(): MonthlyReport = MonthlyReport.of(java.time.YearMonth.now(zone), store.trips.value, store.settings.value.promises, zone)

    // ───────────── 시연 ─────────────

    fun runDemo(s: DemoScenario) {
        if (_demo.value != null) return
        scope.launch {
            _demo.value = s
            guard.reset()
            try {
                when (s) {
                    DemoScenario.PARKING_PEDAL -> {
                        demoNearParking = true
                        set(Gear.DRIVE, 8f, false); say("주차장이에요, 천천히 브레이크 확인하세요."); delay(3_000)
                        set(Gear.DRIVE, 0f, true); delay(1_500)
                        set(Gear.REVERSE, 0f, true); delay(1_500)
                        set(Gear.REVERSE, 2f, false); delay(800)
                        ramp(2f, 16f, 1_200)                       // 브레이크 대신 가속 페달
                        delay(1_000)
                        set(Gear.REVERSE, 16f, true); ramp(16f, 0f, 1_500, brake = true)
                    }
                    DemoScenario.BRAKE_WEAK -> {
                        set(Gear.DRIVE, 25f, false); delay(2_000)
                        demoBrake = true; ramp(25f, 34f, 2_500, brake = true) // 밟았는데 속도가 오름 (내리막·두 페달)
                        delay(1_000); ramp(34f, 0f, 3_000, brake = true)
                    }
                    DemoScenario.LAUNCH -> {
                        set(Gear.DRIVE, 0f, true); delay(2_000)
                        set(Gear.DRIVE, 0f, false); delay(300)
                        ramp(0f, 15f, 1_000)
                        delay(1_000); ramp(15f, 0f, 1_500, brake = true)
                    }
                }
                delay(2_000)
            } finally {
                demoSpeed = null; demoBrake = null; demoGear = null; demoNearParking = false
                _demo.value = null
                guard.reset()
            }
        }
    }

    private fun set(g: Gear, v: Float, brake: Boolean) { demoGear = g; demoSpeed = v; demoBrake = brake }

    private suspend fun ramp(from: Float, to: Float, ms: Long, brake: Boolean = false) {
        demoBrake = brake
        val steps = (ms / 100).toInt().coerceAtLeast(1)
        for (i in 1..steps) { demoSpeed = from + (to - from) * i / steps; delay(100) }
    }
}
