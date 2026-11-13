package com.yikihyub.drivingpromise.domain

/** 내비 회전 안내를 앱이 쓰는 종류로 줄인 것. */
enum class Turn { LEFT, RIGHT, UTURN, ROUNDABOUT, HIGHWAY_IN, ARRIVE, OTHER }

/** 내비 회전 안내 한 건. [exitClock] 은 회전교차로 진출 방향(2·3·4·6·8·9·10·12시). */
data class TurnInfo(val turn: Turn, val distanceM: Double, val description: String = "", val exitClock: Int? = null)

enum class Signal { OFF, LEFT, RIGHT }

/** 주의구간 종류. Pleos CautionPointType 중 고령 운전자에게 의미 있는 것만 문장을 둔다. */
enum class Caution(val speech: String) {
    ACCIDENT_PRONE("이곳은 사고가 잦은 곳이에요. 속도를 줄이고 맞은편 차를 확인하세요."),
    SCHOOL_ZONE("곧 어린이 보호구역이에요. 시속 30킬로미터로 천천히 가요."),
    RAILROAD("곧 철길 건널목이에요. 건너기 전에 꼭 멈추고 좌우를 보세요."),
    SPEED_BUMP("곧 과속방지턱이에요."),
    FOG("안개가 잦은 곳이에요. 전조등을 켜고 천천히 가요."),
    DOWNHILL("긴 내리막이에요. 브레이크를 오래 밟지 말고 속도를 미리 줄여요."),
    NARROW("길이 좁아져요. 천천히 가요."),
    SLIPPERY("미끄러운 길이에요. 천천히 가요."),
    WRONG_WAY("역주행 차가 있다는 알림이에요. 오른쪽 차로로 천천히 가요."),
    OTHER(""),
}

/**
 * 운전 중 짧은 안내 규칙 묶음. 내비·차량 신호 이벤트를 넣으면 읽어줄 문장을 돌려준다.
 * 같은 안내는 한 번만. 시간 값은 호출 측이 넣어 테스트할 수 있게 했다.
 */
class DriveCoach {
    companion object {
        const val LEFT_TURN_PREVIEW_M = 300.0
        const val ROUNDABOUT_PREVIEW_M = 500.0
        const val CAUTION_PREVIEW_M = 300
        const val SIGNAL_REMIND_M = 50.0
        /** 회전이 없는데 깜빡이가 이만큼 켜져 있으면 알림 */
        const val SIGNAL_LEFT_ON_MS = 25_000L
        const val STEER_STRAIGHT_DEG = 4f
        const val PARKING_PREVIEW_M = 300
        const val HEADLIGHT_REMIND_MS = 10 * 60_000L
        const val REST_EVERY_MIN = 60
    }

    /** 한 번 가본 길이면 회전 미리 알림을 줄인다 (사고 다발·스쿨존 같은 위험 안내는 유지). */
    var familiarRoute = false

    private val previewedTurns = mutableSetOf<String>()
    private val remindedSignal = mutableSetOf<String>()
    private val previewedCautions = mutableSetOf<String>()
    private var nextTurn: TurnInfo? = null
    private var signal = Signal.OFF
    private var signalOnSince: Long? = null
    private var signalReminded = false
    private var steerDeg = 0f
    private var parkingSaid = false
    private var lastHeadlightRemind: Long? = null
    private var restSaidAtMin = 0
    private var wiperOn = false

    fun resetTrip() {
        previewedTurns.clear(); remindedSignal.clear(); previewedCautions.clear()
        nextTurn = null; parkingSaid = false; restSaidAtMin = 0; signalReminded = false
    }

    /** 다음 회전 안내가 갱신될 때 */
    fun onTurn(t: TurnInfo, speedKmh: Float): String? {
        nextTurn = t
        val key = "${t.turn}:${t.description}:${t.exitClock}"
        // 1) 좌회전·유턴·회전교차로 미리 알림
        if (!familiarRoute && key !in previewedTurns) {
            val say = when {
                (t.turn == Turn.LEFT || t.turn == Turn.UTURN) && t.distanceM <= LEFT_TURN_PREVIEW_M && t.distanceM > SIGNAL_REMIND_M ->
                    "${roundM(t.distanceM)}미터 앞에서 ${if (t.turn == Turn.LEFT) "좌회전" else "유턴"}이에요. 미리 왼쪽 차로로 가 있어요."
                t.turn == Turn.ROUNDABOUT && t.distanceM <= ROUNDABOUT_PREVIEW_M ->
                    "곧 회전교차로예요. 들어가기 전에 멈추고 왼쪽 차를 먼저 보내세요." + (t.exitClock?.let { " ${clockToExit(it)}로 나가요." } ?: "")
                else -> null
            }
            if (say != null) { previewedTurns += key; return say }
        }
        // 2) 회전 직전인데 깜빡이 안 켬
        if (t.distanceM <= SIGNAL_REMIND_M && speedKmh > 5f && key !in remindedSignal) {
            val need = when (t.turn) { Turn.LEFT, Turn.UTURN -> Signal.LEFT; Turn.RIGHT -> Signal.RIGHT; else -> null }
            if (need != null && signal != need) {
                remindedSignal += key
                return if (need == Signal.LEFT) "왼쪽 깜빡이 켜세요." else "오른쪽 깜빡이 켜세요."
            }
        }
        return null
    }

    fun onSignal(s: Signal, nowMs: Long) {
        if (s != signal) { signalOnSince = if (s == Signal.OFF) null else nowMs; signalReminded = false }
        signal = s
    }

    fun onSteering(deg: Float) { steerDeg = deg }

    /** 주기적으로(1초) 불러 깜빡이 끄기 알림을 확인한다. */
    fun checkSignalLeftOn(nowMs: Long, speedKmh: Float): String? {
        val since = signalOnSince ?: return null
        if (signalReminded || speedKmh < 15f) return null
        val upcoming = nextTurn?.let { it.distanceM < 150 && it.turn in setOf(Turn.LEFT, Turn.RIGHT, Turn.UTURN, Turn.ROUNDABOUT) } ?: false
        if (upcoming) return null
        if (nowMs - since >= SIGNAL_LEFT_ON_MS && kotlin.math.abs(steerDeg) < STEER_STRAIGHT_DEG) {
            signalReminded = true
            return "깜빡이가 계속 켜져 있어요."
        }
        return null
    }

    fun onCaution(c: Caution, remainM: Int, id: String): String? {
        if (c == Caution.OTHER || remainM > CAUTION_PREVIEW_M || id in previewedCautions) return null
        if (familiarRoute && c in setOf(Caution.SPEED_BUMP, Caution.NARROW)) return null
        previewedCautions += id
        return c.speech
    }

    /** 목적지까지 남은 거리. 도착 직전 주차장 안내. 도착 직전이면 true 를 [nearParking] 으로 쓴다. */
    fun onRemainDistance(m: Int): String? {
        if (m in 1..PARKING_PREVIEW_M && !parkingSaid) {
            parkingSaid = true
            return "곧 도착해요. 주차장에서는 천천히, 브레이크 확인하세요."
        }
        return null
    }

    fun nearParking(remainM: Int?): Boolean = remainM != null && remainM in 1..PARKING_PREVIEW_M

    /** 와이퍼가 켜지는 순간 한 번 */
    fun onWiper(on: Boolean, lowBeamOn: Boolean?): String? {
        val turnedOn = on && !wiperOn
        wiperOn = on
        if (!turnedOn) return null
        return if (lowBeamOn == false) "비가 와요. 전조등을 켜고, 앞 유리가 뿌예지면 김서림 버튼을 누르세요."
        else "비가 와요. 차간 거리를 넉넉히 두고 천천히 가요."
    }

    /** 해가 졌는데 전조등이 꺼져 있으면. 10분에 한 번까지 */
    fun checkHeadlight(dark: Boolean, lowBeamOn: Boolean?, speedKmh: Float, nowMs: Long): String? {
        if (!dark || lowBeamOn != false || speedKmh < 5f) return null
        val last = lastHeadlightRemind
        if (last != null && nowMs - last < HEADLIGHT_REMIND_MS) return null
        lastHeadlightRemind = nowMs
        return "해가 졌어요. 전조등을 켜세요."
    }

    /** 연속 주행 시간(분). 1시간마다 쉬기 약속 */
    fun onDrivingMinutes(min: Int, promises: Set<Promise>): String? {
        if (Promise.REST_HOURLY !in promises) return null
        val due = (min / REST_EVERY_MIN) * REST_EVERY_MIN
        if (due >= REST_EVERY_MIN && due > restSaidAtMin) {
            restSaidAtMin = due
            return "${min / 60}시간째 운전 중이에요. 가까운 휴게소나 쉼터에서 쉬어 가요."
        }
        return null
    }

    private fun roundM(m: Double) = ((m / 50).toInt() * 50).coerceAtLeast(50)

    private fun clockToExit(clock: Int) = when (clock) {
        in 1..3 -> "첫 번째 출구"
        in 4..7 -> "두 번째 출구"
        in 8..10 -> "세 번째 출구"
        else -> "네 번째 출구"
    }
}

/** 출발 전 경로 미리보기: "가는 길에 회전교차로 1곳, 사고가 잦은 곳 1곳이 있어요" */
object RoutePreview {
    fun speech(turns: List<TurnInfo>, cautions: List<Caution>): String? {
        val parts = buildList {
            val r = turns.count { it.turn == Turn.ROUNDABOUT }
            val l = turns.count { it.turn == Turn.LEFT || it.turn == Turn.UTURN }
            val a = cautions.count { it == Caution.ACCIDENT_PRONE }
            val s = cautions.count { it == Caution.SCHOOL_ZONE }
            if (r > 0) add("회전교차로 ${r}곳")
            if (l > 0) add("좌회전 ${l}번")
            if (a > 0) add("사고가 잦은 곳 ${a}곳")
            if (s > 0) add("어린이 보호구역 ${s}곳")
        }
        if (parts.isEmpty()) return null
        return "가는 길에 ${parts.joinToString(", ")}이 있어요. 가까워지면 미리 알려드릴게요."
    }
}
