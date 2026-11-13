package com.yikihyub.drivingpromise.domain

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** 운행 한 번의 기록. 차 안에만 저장한다. */
data class Trip(
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val destination: String = "",
    /** 해 진 뒤 달린 적이 있는지 */
    val night: Boolean = false,
    /** 와이퍼가 켜진 적이 있는지 */
    val rain: Boolean = false,
    /** 고속도로 진입 안내가 있었는지 */
    val highway: Boolean = false,
    /** 쉬지 않고 달린 가장 긴 시간(분) */
    val longestStintMin: Int = 0,
    /** 밤 출발 때 동승자가 있다고 답했는지. 묻지 않았으면 null */
    val companion: Boolean? = null,
    val pedalAlerts: Int = 0,
) {
    val durationMin: Int get() = ((endMs - startMs) / 60_000).toInt()

    /** 이 운행이 그 약속을 지켰는지 */
    fun kept(p: Promise): Boolean = when (p) {
        Promise.NO_NIGHT -> !night
        Promise.AVOID_HIGHWAY -> !highway
        Promise.RAIN_NEAR_ONLY -> !(rain && distanceKm > RAIN_NEAR_KM)
        Promise.REST_HOURLY -> longestStintMin <= 70
        Promise.NIGHT_WITH_COMPANION -> !night || companion == true
    }
}

data class PromiseScore(val promise: Promise, val kept: Int, val total: Int)

data class MonthlyReport(
    val month: YearMonth,
    val trips: Int,
    val distanceKm: Double,
    val nightTrips: Int,
    val rainTrips: Int,
    val longestStintMin: Int,
    val pedalAlerts: Int,
    val scores: List<PromiseScore>,
) {
    val keptTotal get() = scores.sumOf { it.kept }
    val promiseTotal get() = scores.sumOf { it.total }

    /** 가족에게 보낼 한 줄. 남과 비교하지 않고 내 약속 기준만. */
    fun familySummary(): String =
        if (trips == 0) "${month.monthValue}월에는 아직 운전 기록이 없어요."
        else "${month.monthValue}월 운전 ${trips}번, 약속 ${promiseTotal}번 중 ${keptTotal}번 지킴, 페달 확인 안내 ${pedalAlerts}회"

    fun speech(): String =
        if (trips == 0) "이번 달은 아직 운전 기록이 없어요."
        else "이번 달 ${trips}번 운전했어요. 약속 ${promiseTotal}번 중 ${keptTotal}번 지켰어요." +
            (if (nightTrips == 0) " 밤 운전은 한 번도 없었어요." else " 밤 운전은 ${nightTrips}번이었어요.")

    companion object {
        fun of(month: YearMonth, trips: List<Trip>, promises: Set<Promise>, zone: ZoneId = ZoneId.systemDefault()): MonthlyReport {
            val inMonth = trips.filter { YearMonth.from(Instant.ofEpochMilli(it.startMs).atZone(zone)) == month }
            return MonthlyReport(
                month = month,
                trips = inMonth.size,
                distanceKm = inMonth.sumOf { it.distanceKm },
                nightTrips = inMonth.count { it.night },
                rainTrips = inMonth.count { it.rain },
                longestStintMin = inMonth.maxOfOrNull { it.longestStintMin } ?: 0,
                pedalAlerts = inMonth.sumOf { it.pedalAlerts },
                scores = Promise.entries.filter { it in promises }.map { p -> PromiseScore(p, inMonth.count { it.kept(p) }, inMonth.size) },
            )
        }
    }
}

/** 페달 확인 안내가 울렸거나 급정거한 곳. 다음에 가까워지면 미리 알려준다. */
data class SurpriseSpot(
    val lat: Double,
    val lng: Double,
    val timeMs: Long,
    val reason: String,
    val place: String = "",
) {
    companion object {
        /** 이 거리 안의 기존 지점이 있으면 새로 만들지 않는다 */
        const val MERGE_M = 80.0
        const val ALERT_RADIUS_M = 150f

        fun distanceM(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(bLat - aLat)
            val dLng = Math.toRadians(bLng - aLng)
            val h = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(aLat)) * kotlin.math.cos(Math.toRadians(bLat)) * kotlin.math.sin(dLng / 2).let { it * it }
            return 2 * r * kotlin.math.asin(kotlin.math.sqrt(h))
        }

        fun merge(existing: List<SurpriseSpot>, new: SurpriseSpot): List<SurpriseSpot> =
            if (existing.any { distanceM(it.lat, it.lng, new.lat, new.lng) < MERGE_M }) existing else existing + new
    }
}
