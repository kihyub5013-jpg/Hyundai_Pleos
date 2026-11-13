package com.yikihyub.drivingpromise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PromisesTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    @Test fun `서울 동지 일몰은 오후 5시 10분 무렵`() {
        val s = Sun.sunset(LocalDate.of(2026, 12, 21), 37.5665, 126.978, seoul)
        assertNotNull(s)
        val min = s!!.hour * 60 + s.minute
        assertTrue("got $s", min in (17 * 60 + 5)..(17 * 60 + 25))
    }

    @Test fun `서울 하지 일몰은 오후 7시 50분 무렵`() {
        val s = Sun.sunset(LocalDate.of(2026, 6, 21), 37.5665, 126.978, seoul)!!
        val min = s.hour * 60 + s.minute
        assertTrue("got $s", min in (19 * 60 + 45)..(20 * 60 + 2))
        assertEquals(LocalDate.of(2026, 6, 21), s.toLocalDate())
    }

    @Test fun `시나리오 2 - 4시 출발 1시간 걸리고 일몰 5시 20분이면 20분 전 도착`() {
        val now = ZonedDateTime.of(2026, 12, 1, 16, 0, 0, 0, seoul)
        val sunset = now.withHour(17).withMinute(20)
        val v = DepartureCheck.sunset(now, 3600, sunset)
        assertEquals(SunsetVerdict.InTime(20, now.plusHours(1)), v)
        assertTrue(v.speech.contains("20분 전"))
    }

    @Test fun `일몰 뒤 도착이면 늦는다고 말한다`() {
        val now = ZonedDateTime.of(2026, 12, 1, 16, 50, 0, 0, seoul)
        val v = DepartureCheck.sunset(now, 3600, now.withHour(17).withMinute(20))
        assertTrue(v is SunsetVerdict.Late && v.lateMin == 30L)
    }

    @Test fun `이미 해가 졌으면`() {
        val now = ZonedDateTime.of(2026, 12, 1, 18, 0, 0, 0, seoul)
        assertEquals(SunsetVerdict.AlreadyDark, DepartureCheck.sunset(now, 600, now.withHour(17)))
    }

    @Test fun `말로 약속 정하기`() {
        assertEquals(setOf(Promise.NO_NIGHT, Promise.AVOID_HIGHWAY), Promise.parse("밤에는 운전 안 하고 고속도로도 피할래"))
        assertEquals(setOf(Promise.NIGHT_WITH_COMPANION), Promise.parse("밤엔 동승자 있을 때만"))
        assertEquals(setOf(Promise.RAIN_NEAR_ONLY), Promise.parse("비 오면 가까운 데만 갈게"))
    }

    @Test fun `비 오는 날 먼 목적지면 한마디`() {
        assertNotNull(DepartureCheck.rain(setOf(Promise.RAIN_NEAR_ONLY), true, 40.0))
        assertEquals(null, DepartureCheck.rain(setOf(Promise.RAIN_NEAR_ONLY), true, 5.0))
        assertEquals(null, DepartureCheck.rain(emptySet(), true, 40.0))
    }

    @Test fun `음성 명령`() {
        assertEquals(VoiceCommand.GoHome, VoiceCommand.parse("집에 가자"))
        assertEquals(VoiceCommand.GoTo("서울대학교병원"), VoiceCommand.parse("서울대학교병원으로 가자"))
        assertEquals(VoiceCommand.HowFar, VoiceCommand.parse("얼마나 남았어?"))
        assertEquals(VoiceCommand.MonthReport, VoiceCommand.parse("이번 달 어땠어"))
        assertEquals(VoiceCommand.SetPromises(setOf(Promise.NO_NIGHT)), VoiceCommand.parse("밤에는 운전 안 하는 약속"))
        assertEquals(VoiceCommand.Yes(true), VoiceCommand.parse("네"))
    }

    @Test fun `오후 시각 말하기`() {
        assertEquals("오후 5시 20분", ZonedDateTime.of(2026, 1, 1, 17, 20, 0, 0, seoul).spoken())
        assertEquals("오전 12시", ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, seoul).spoken())
    }
}
