package com.yikihyub.drivingpromise.domain

/** 기어. Pleos VehicleGear 를 앱 안에서 쓰기 쉬운 네 가지로 줄인 것. */
enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/** 안내 민감도. 운전자가 고른다. 숫자가 작을수록 덜 울린다. */
enum class Sensitivity(val label: String, val factor: Float) {
    LOW("덜 민감", 1.35f),
    NORMAL("보통", 1.0f),
    HIGH("민감", 0.75f),
}

/** 페달 확인 안내 종류와 읽어줄 문장. 문장은 짧게, ON_DEVICE TTS 로 바로 읽는다. */
enum class PedalAlert(val speech: String, val title: String) {
    /** 브레이크 없이 속도가 빠르게 오름 → 가속 페달을 세게 밟고 있음 */
    GAS_INSTEAD_OF_BRAKE("지금 엑셀이에요! 브레이크로 바꾸세요", "지금 엑셀이에요"),
    /** 브레이크가 밟혔는데 속도가 계속 오름 → 덜 밟았거나 두 페달을 함께 밟음 */
    BRAKE_NOT_ENOUGH("브레이크 끝까지 꾹 밟으세요", "브레이크 끝까지"),
    /** 정차 직후·주차장에서 브레이크 없이 급출발 */
    LURCH_FROM_STOP("엑셀 밟고 계세요, 발 확인하세요", "발 확인하세요"),
}

/**
 * 차에서 들어오는 신호 한 묶음.
 * @param speedKmh 차속(km/h). 후진도 양수.
 * @param brake 브레이크 페달 밟힘 여부. 모르면 null.
 * @param nearParking 목적지 도착 직전이거나 주차장(도착 후 5분 이내 등)이라 민감 구간인지.
 */
data class PedalSample(
    val timeMs: Long,
    val speedKmh: Float,
    val brake: Boolean?,
    val gear: Gear,
    val nearParking: Boolean = false,
)

/**
 * 페달 확인 안내 판단. 차량을 제어하지 않고, 울릴지 말지만 정한다.
 *
 * 속도 변화(가속도)와 브레이크 상태를 비교한다. 가속 페달 신호는 SDK 에 없어서 간접 판단이다.
 *  - 민감 구간(주차장, 20km/h 이하, 정차 후 출발 10초 이내, 후진, 도착 직전)에서는 낮은 가속에도 울린다.
 *  - 일반 도로에서는 극단적인 가속에만 울려 고속도로 합류 같은 정상 가속은 넘어간다.
 *  - 같은 안내는 [cooldownMs] 동안 다시 울리지 않는다. 자주 울리면 운전자가 꺼버린다.
 */
class PedalGuard(
    var sensitivity: Sensitivity = Sensitivity.NORMAL,
    private val cooldownMs: Long = 4_000,
) {
    companion object {
        /** 가속도를 잴 최소 구간. 신호 잡음을 줄인다. */
        const val WINDOW_MS = 600L
        const val HISTORY_MS = 3_000L
        const val SLOW_KMH = 20f
        const val LAUNCH_WINDOW_MS = 10_000L

        /** km/h per s. 보통 출발은 4~7(1.1~1.9 m/s²). 페달 오조작(풀스로틀)은 11 이상. */
        const val SENSITIVE_ACCEL = 9f
        const val EXTREME_ACCEL = 16f
        const val BRAKE_BUT_RISING = 2.5f
        /** 정지 상태에서 1.5초 안에 이 속도를 넘기면 급출발 (평균 8km/h/s) */
        const val LURCH_KMH = 12f
        const val LURCH_WINDOW_MS = 1_500L
        /** 이보다 크게 속도가 줄면 "급정거" — 깜짝 지점으로 저장한다 */
        const val HARD_DECEL = 14f
    }

    private val history = ArrayDeque<PedalSample>()
    private var lastStopMs: Long? = null
    private val lastAlertAt = mutableMapOf<PedalAlert, Long>()

    /** 마지막으로 계산한 가속도(km/h/s). 화면 디버그용 */
    var lastAccel: Float = 0f
        private set

    /** 마지막 샘플에서 급정거가 감지됐는지 */
    var hardStop: Boolean = false
        private set

    @Synchronized
    fun reset() {
        history.clear(); lastStopMs = null; lastAlertAt.clear(); lastAccel = 0f; hardStop = false
    }

    fun isSensitive(s: PedalSample): Boolean {
        val launching = lastStopMs?.let { s.timeMs - it <= LAUNCH_WINDOW_MS } ?: false
        return s.nearParking || s.gear == Gear.REVERSE || s.speedKmh <= SLOW_KMH || launching
    }

    /** 새 샘플을 넣고, 울려야 하면 안내 종류를 돌려준다. */
    @Synchronized
    fun onSample(s: PedalSample): PedalAlert? {
        if (s.gear == Gear.PARK) { history.clear(); lastStopMs = s.timeMs; hardStop = false; return null }
        history.addLast(s)
        while (history.isNotEmpty() && s.timeMs - history.first().timeMs > HISTORY_MS) history.removeFirst()
        if (s.speedKmh < 1f) lastStopMs = s.timeMs

        val ref = history.lastOrNull { s.timeMs - it.timeMs >= WINDOW_MS } ?: return null
        val dt = (s.timeMs - ref.timeMs) / 1000f
        val accel = (s.speedKmh - ref.speedKmh) / dt
        lastAccel = accel
        hardStop = accel <= -HARD_DECEL

        val f = sensitivity.factor
        val sensitive = isSensitive(s)
        val brakeOn = s.brake == true
        // 브레이크 신호가 이 구간 내내 켜져 있었는지 (밟자마자 한 프레임은 관성으로 속도가 오를 수 있다)
        val brakeHeld = brakeOn && history.filter { it.timeMs >= ref.timeMs }.all { it.brake == true }

        val alert = when {
            brakeHeld && accel >= BRAKE_BUT_RISING * f && s.speedKmh > 3f -> PedalAlert.BRAKE_NOT_ENOUGH
            !brakeOn && sensitive && s.gear != Gear.REVERSE && isLurch(s, accel) -> PedalAlert.LURCH_FROM_STOP
            !brakeOn && sensitive && accel >= SENSITIVE_ACCEL * f -> PedalAlert.GAS_INSTEAD_OF_BRAKE
            !brakeOn && accel >= EXTREME_ACCEL * f -> PedalAlert.GAS_INSTEAD_OF_BRAKE
            else -> null
        } ?: return null

        val last = lastAlertAt[alert]
        if (last != null && s.timeMs - last < cooldownMs) return null
        lastAlertAt[alert] = s.timeMs
        return alert
    }

    /** 정차 후 1.5초 안에: 이미 빨라졌거나, 지금 가속이 민감 기준을 넘음. 후진은 "엑셀" 안내가 더 분명하다. */
    private fun isLurch(s: PedalSample, accel: Float): Boolean {
        val stoppedAt = lastStopMs ?: return false
        if (s.timeMs - stoppedAt > LURCH_WINDOW_MS) return false
        val f = sensitivity.factor
        return s.speedKmh >= LURCH_KMH * f || accel >= SENSITIVE_ACCEL * f
    }
}
