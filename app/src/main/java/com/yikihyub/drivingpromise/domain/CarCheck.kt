package com.yikihyub.drivingpromise.domain

/** 액체 레벨. Pleos OilLevelState 와 같은 값. */
enum class Level { NORMAL, LOW, CRITICALLY_LOW, HIGH, ERROR, UNKNOWN }

/** 출발 전 차 상태 스냅샷. 모르는 값은 null/UNKNOWN 으로 두고 문장에서 뺀다. */
data class CarStatus(
    val engineOil: Level = Level.UNKNOWN,
    val brakeFluid: Level = Level.UNKNOWN,
    /** 바퀴별 저압 경고 여부. 지원 안 하면 빈 목록 */
    val lowTireWheels: List<String> = emptyList(),
    val fuelPercent: Int? = null,
    val evSocPercent: Int? = null,
    /** 켜져 있는 빨간 경고등 이름 (권한 있는 차에서만) */
    val redTelltales: List<String> = emptyList(),
    val yellowTelltales: List<String> = emptyList(),
)

data class CarBrief(
    /** 한 문장 요약. TTS 로 읽는다 */
    val speech: String,
    /** 화면에 보일 항목별 상태 */
    val items: List<Pair<String, String>>,
    /** 정비소를 경유지로 제안할 만큼 문제가 있는지 */
    val suggestService: Boolean,
    /** 주유소·충전소 제안 */
    val suggestEnergy: Boolean,
)

object CarCheck {
    const val LOW_ENERGY_PERCENT = 15

    fun brief(s: CarStatus): CarBrief {
        val problems = mutableListOf<String>()
        var service = false

        if (s.redTelltales.isNotEmpty()) { problems += "빨간 경고등(${s.redTelltales.joinToString()})이 켜져 있어요"; service = true }
        if (s.brakeFluid == Level.LOW || s.brakeFluid == Level.CRITICALLY_LOW) { problems += "브레이크액이 부족해요"; service = true }
        if (s.engineOil == Level.LOW || s.engineOil == Level.CRITICALLY_LOW) { problems += "엔진오일이 부족해요"; service = true }
        if (s.lowTireWheels.isNotEmpty()) { problems += "${s.lowTireWheels.joinToString()} 타이어 바람이 빠졌어요"; service = true }
        if (s.yellowTelltales.isNotEmpty()) problems += "노란 경고등(${s.yellowTelltales.joinToString()})이 켜져 있어요"

        val energy = s.evSocPercent ?: s.fuelPercent
        val energyName = if (s.evSocPercent != null) "배터리" else "연료"
        val lowEnergy = energy != null && energy <= LOW_ENERGY_PERCENT
        if (lowEnergy) problems += "${energyName}가 ${energy}퍼센트 남았어요"

        val speech = when {
            problems.isEmpty() && energy != null -> "오늘 차 상태 좋아요. ${energyName} ${energy}퍼센트예요."
            problems.isEmpty() -> "오늘 차 상태 좋아요."
            service -> problems.joinToString(". ") + ". 가는 길에 정비소에 들를까요?"
            lowEnergy -> problems.joinToString(". ") + ". 가는 길에 ${if (s.evSocPercent != null) "충전소" else "주유소"}에 들를까요?"
            else -> problems.joinToString(". ") + "."
        }

        val items = buildList {
            add("엔진오일" to s.engineOil.label())
            add("브레이크액" to s.brakeFluid.label())
            // Pleos Tire API(getLowTirePressure)는 현재 공기압이 아니라 저압 임계값만 준다. 경고가 들어올 때만 표시.
            if (s.lowTireWheels.isNotEmpty()) add("타이어" to "${s.lowTireWheels.joinToString()} 저압")
            add(energyName to (energy?.let { "$it%" } ?: "확인 불가"))
            add("경고등" to when {
                s.redTelltales.isNotEmpty() -> "빨강 ${s.redTelltales.size}개"
                s.yellowTelltales.isNotEmpty() -> "노랑 ${s.yellowTelltales.size}개"
                else -> "없음"
            })
        }
        return CarBrief(speech, items, service, lowEnergy)
    }

    private fun Level.label() = when (this) {
        Level.NORMAL, Level.HIGH -> "정상"
        Level.LOW -> "부족"
        Level.CRITICALLY_LOW -> "매우 부족"
        Level.ERROR -> "센서 오류"
        Level.UNKNOWN -> "확인 불가"
    }
}
