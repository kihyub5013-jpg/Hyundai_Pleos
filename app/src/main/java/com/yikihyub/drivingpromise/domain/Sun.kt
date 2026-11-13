package com.yikihyub.drivingpromise.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * 위치 기반 일출·일몰 계산 (NOAA 간이식). 네트워크 없이 차 안에서 바로 계산한다.
 * 오차는 수 분 이내라 "해 지기 전 도착" 판단에 충분하다.
 */
object Sun {
    private const val ZENITH = 90.833 // 대기 굴절·태양 반지름 보정

    /** 그날 일몰 시각. 백야/극야처럼 해가 지지 않거나 뜨지 않으면 null. */
    fun sunset(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): ZonedDateTime? = calc(date, lat, lng, zone, rise = false)

    fun sunrise(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): ZonedDateTime? = calc(date, lat, lng, zone, rise = true)

    /** 지금이 해 진 뒤(일몰~다음 일출)인지 */
    fun isDark(now: ZonedDateTime, lat: Double, lng: Double): Boolean {
        val d = now.toLocalDate()
        val rise = sunrise(d, lat, lng, now.zone) ?: return false
        val set = sunset(d, lat, lng, now.zone) ?: return false
        return now.isBefore(rise) || !now.isBefore(set)
    }

    private fun calc(date: LocalDate, lat: Double, lng: Double, zone: ZoneId, rise: Boolean): ZonedDateTime? {
        val n = date.dayOfYear
        val lngHour = lng / 15.0
        val t = n + ((if (rise) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634
        l = norm(l, 360.0)
        var ra = deg(kotlin.math.atan(0.91764 * tan(rad(l))))
        ra = norm(ra, 360.0)
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra = (ra + (lQuadrant - raQuadrant)) / 15.0
        val sinDec = 0.39782 * sin(rad(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(rad(ZENITH)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
        if (cosH > 1 || cosH < -1) return null
        val h = (if (rise) 360.0 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
        val localMean = h + ra - 0.06571 * t - 6.622
        val utHours = norm(localMean - lngHour, 24.0)
        val seconds = (utHours * 3600).toLong()
        val local = date.atStartOfDay(ZoneId.of("UTC")).plusSeconds(seconds).withZoneSameInstant(zone)
        // UTC 기준으로 계산했으므로 현지 날짜가 하루 어긋날 수 있다. 요청한 날짜로 맞춘다.
        return local.plusDays(java.time.temporal.ChronoUnit.DAYS.between(local.toLocalDate(), date))
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun norm(v: Double, m: Double) = ((v % m) + m) % m
}
