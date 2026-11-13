package com.yikihyub.drivingpromise.domain

import java.time.Duration
import java.time.ZonedDateTime

/** 운전자가 스스로 정하는 약속. 조건부 면허가 검토하는 조건(야간·고속도로·거리)과 같은 축. */
enum class Promise(val title: String, val detail: String, val keywords: List<String>) {
    NO_NIGHT("해 진 뒤 운전 안 함", "출발 전에 도착 시각을 일몰과 비교해요", listOf("밤", "야간", "해 지", "해지", "어두")),
    AVOID_HIGHWAY("고속도로 피함", "편한 길·무료도로 우선으로 길을 찾아요", listOf("고속도로", "고속")),
    RAIN_NEAR_ONLY("비 오면 가까운 곳만", "와이퍼가 켜지면 남은 거리를 확인해요", listOf("비오", "비가", "비올", "비내", "우천", "빗길")),
    REST_HOURLY("1시간마다 쉬기", "연속 운전 1시간이 되면 쉬어 가자고 말해요", listOf("쉬", "휴식", "한 시간", "1시간")),
    NIGHT_WITH_COMPANION("밤엔 동승자 있을 때만", "밤에 출발하면 동승자가 있는지 물어봐요", listOf("동승", "같이 탈", "옆자리")),
    ;

    companion object {
        /** "밤에는 운전 안 할래, 고속도로도 피하고" 같은 말에서 약속을 고른다. */
        fun parse(said: String): Set<Promise> {
            val s = said.replace(" ", "")
            return entries.filter { p -> p.keywords.any { s.contains(it.replace(" ", "")) } }
                // "밤엔 동승자"는 NO_NIGHT 와 겹치는 말이 있으니 동승 언급이 있으면 동승자 약속으로
                .let { found -> if (NIGHT_WITH_COMPANION in found) found - NO_NIGHT else found }
                .toSet()
        }
    }
}

/** 비 오는 날 "가까운 곳"의 기준 (km). */
const val RAIN_NEAR_KM = 15

/** 출발 전 "해 지기 전 도착" 판단 결과 */
sealed interface SunsetVerdict {
    val speech: String

    /** 해 지기 [marginMin]분 전에 도착 */
    data class InTime(val marginMin: Long, val arrival: ZonedDateTime) : SunsetVerdict {
        override val speech = "지금 출발하면 해 지기 ${marginMin}분 전에 도착해요."
    }

    /** 일몰 뒤 [lateMin]분 늦게 도착. [leaveBy] 까지 출발했어야 함 */
    data class Late(val lateMin: Long, val arrival: ZonedDateTime, val sunset: ZonedDateTime) : SunsetVerdict {
        override val speech = "지금 가면 해 지고 ${lateMin}분 뒤에 도착해요. 밤 운전 약속이 있어요. 가까운 곳에서 쉬었다 내일 가거나, 가족에게 부탁해 보세요."
    }

    /** 이미 해가 졌음 */
    data object AlreadyDark : SunsetVerdict {
        override val speech = "벌써 해가 졌어요. 오늘은 밤 운전 약속을 지키기 어려워요. 천천히, 전조등 확인하고 가세요."
    }

    /** 일몰 계산 불가 / 목적지 없음 */
    data object Unknown : SunsetVerdict {
        override val speech = ""
    }
}

object DepartureCheck {
    /** 경로 상의 남은 시간(초)과 오늘 일몰로 판단한다. 여유 10분 미만이면 늦는 것으로 본다. */
    fun sunset(now: ZonedDateTime, remainTimeS: Int, sunset: ZonedDateTime?, safetyMarginMin: Long = 10): SunsetVerdict {
        if (sunset == null || remainTimeS <= 0) return SunsetVerdict.Unknown
        if (!now.isBefore(sunset)) return SunsetVerdict.AlreadyDark
        val arrival = now.plusSeconds(remainTimeS.toLong())
        val margin = Duration.between(arrival, sunset).toMinutes()
        return if (margin >= safetyMarginMin) SunsetVerdict.InTime(margin, arrival)
        else SunsetVerdict.Late(maxOf(1, -margin), arrival, sunset)
    }

    /** 출발 시 비 약속: 와이퍼가 켜져 있는데 목적지가 멀면 한마디 */
    fun rain(promises: Set<Promise>, wiperOn: Boolean, remainKm: Double?): String? {
        if (Promise.RAIN_NEAR_ONLY !in promises || !wiperOn || remainKm == null) return null
        return if (remainKm > RAIN_NEAR_KM) "비가 와요. 목적지까지 ${remainKm.toInt()}킬로미터라 비 오는 날 약속보다 멀어요. 꼭 가야 하는 길인지 한 번 더 생각해 보세요."
        else null
    }
}

/** 시간을 말로: 17:20 → "오후 5시 20분" */
fun ZonedDateTime.spoken(): String {
    val h = hour
    val ampm = if (h < 12) "오전" else "오후"
    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return if (minute == 0) "$ampm ${h12}시" else "$ampm ${h12}시 ${minute}분"
}
