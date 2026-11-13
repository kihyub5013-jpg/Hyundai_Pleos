package com.yikihyub.drivingpromise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class CoachTest {

    @Test fun `좌회전 300m 전에 한 번만 미리 알림`() {
        val c = DriveCoach()
        assertNull(c.onTurn(TurnInfo(Turn.LEFT, 600.0, "좌회전"), 40f))
        val s = c.onTurn(TurnInfo(Turn.LEFT, 280.0, "좌회전"), 40f)
        assertTrue(s!!.contains("좌회전"))
        assertNull(c.onTurn(TurnInfo(Turn.LEFT, 200.0, "좌회전"), 40f))
    }

    @Test fun `회전 직전 깜빡이를 안 켜면 알림, 켜면 조용`() {
        val c = DriveCoach().apply { familiarRoute = true }
        assertEquals("왼쪽 깜빡이 켜세요.", c.onTurn(TurnInfo(Turn.LEFT, 40.0, "a"), 20f))
        val c2 = DriveCoach().apply { familiarRoute = true }
        c2.onSignal(Signal.LEFT, 0)
        assertNull(c2.onTurn(TurnInfo(Turn.LEFT, 40.0, "a"), 20f))
    }

    @Test fun `회전이 없는데 깜빡이가 오래 켜져 있으면`() {
        val c = DriveCoach()
        c.onSignal(Signal.RIGHT, 0)
        c.onSteering(0f)
        assertNull(c.checkSignalLeftOn(10_000, 50f))
        assertEquals("깜빡이가 계속 켜져 있어요.", c.checkSignalLeftOn(26_000, 50f))
        assertNull(c.checkSignalLeftOn(40_000, 50f))
    }

    @Test fun `회전교차로 출구 안내`() {
        val s = DriveCoach().onTurn(TurnInfo(Turn.ROUNDABOUT, 450.0, "", exitClock = 9), 30f)
        assertTrue(s!!.contains("세 번째 출구"))
    }

    @Test fun `사고다발 구간은 300m 안에서 한 번`() {
        val c = DriveCoach()
        assertNull(c.onCaution(Caution.ACCIDENT_PRONE, 800, "x"))
        assertEquals(Caution.ACCIDENT_PRONE.speech, c.onCaution(Caution.ACCIDENT_PRONE, 290, "x"))
        assertNull(c.onCaution(Caution.ACCIDENT_PRONE, 100, "x"))
    }

    @Test fun `1시간마다 쉬기`() {
        val c = DriveCoach()
        val p = setOf(Promise.REST_HOURLY)
        assertNull(c.onDrivingMinutes(59, p))
        assertTrue(c.onDrivingMinutes(60, p)!!.contains("1시간째"))
        assertNull(c.onDrivingMinutes(75, p))
        assertTrue(c.onDrivingMinutes(121, p)!!.contains("2시간째"))
        assertNull(DriveCoach().onDrivingMinutes(60, emptySet()))
    }

    @Test fun `와이퍼가 켜지는 순간 전조등이 꺼져 있으면`() {
        val c = DriveCoach()
        assertTrue(c.onWiper(true, false)!!.contains("전조등"))
        assertNull(c.onWiper(true, false))
    }

    @Test fun `경로 미리보기`() {
        val s = RoutePreview.speech(listOf(TurnInfo(Turn.ROUNDABOUT, 1000.0), TurnInfo(Turn.RIGHT, 2000.0)), listOf(Caution.ACCIDENT_PRONE))
        assertEquals("가는 길에 회전교차로 1곳, 사고가 잦은 곳 1곳이 있어요. 가까워지면 미리 알려드릴게요.", s)
    }

    @Test fun `차 상태 한마디`() {
        assertEquals("오늘 차 상태 좋아요. 연료 60퍼센트예요.", CarCheck.brief(CarStatus(Level.NORMAL, Level.NORMAL, fuelPercent = 60)).speech)
        val bad = CarCheck.brief(CarStatus(Level.LOW, Level.NORMAL, fuelPercent = 60))
        assertTrue(bad.suggestService && bad.speech.contains("정비소"))
        val low = CarCheck.brief(CarStatus(evSocPercent = 10))
        assertTrue(low.suggestEnergy && low.speech.contains("충전소"))
    }

    @Test fun `이달의 약속 - 시나리오 3 문장`() {
        val z = ZoneId.of("Asia/Seoul")
        val base = ZonedDateTime.of(2026, 11, 2, 9, 0, 0, 0, z).toInstant().toEpochMilli()
        val trips = (0 until 10).map { i -> Trip(base + i * 86_400_000L, base + i * 86_400_000L + 1_800_000, 12.0, night = i == 3) }
        val r = MonthlyReport.of(YearMonth.of(2026, 11), trips, setOf(Promise.NO_NIGHT, Promise.AVOID_HIGHWAY, Promise.REST_HOURLY), z)
        assertEquals(30, r.promiseTotal)
        assertEquals(29, r.keptTotal)
        assertEquals("11월 운전 10번, 약속 30번 중 29번 지킴, 페달 확인 안내 0회", r.familySummary())
    }

    @Test fun `깜짝 지점은 가까우면 합친다`() {
        val a = SurpriseSpot(37.5, 127.0, 0, "x")
        val near = SurpriseSpot(37.5003, 127.0, 1, "y") // 약 33m
        val far = SurpriseSpot(37.51, 127.0, 2, "z")
        assertEquals(1, SurpriseSpot.merge(listOf(a), near).size)
        assertEquals(2, SurpriseSpot.merge(listOf(a), far).size)
    }

    @Test fun `터널·톨게이트·지하차도는 가본 길이어도 한 번 안내`() {
        val c = DriveCoach().apply { familiarRoute = true }
        assertNull(c.onTurn(TurnInfo(Turn.TUNNEL, 600.0, "t"), 50f))
        assertTrue(c.onTurn(TurnInfo(Turn.TUNNEL, 280.0, "t"), 50f)!!.contains("전조등"))
        assertNull(c.onTurn(TurnInfo(Turn.TUNNEL, 100.0, "t"), 50f))
        assertTrue(c.onTurn(TurnInfo(Turn.TOLL, 450.0, "g"), 50f)!!.contains("하이패스"))
    }

    @Test fun `비 오는 날 지하차도는 침수 경고`() {
        val c = DriveCoach()
        c.onWiper(true, true)
        assertTrue(c.onTurn(TurnInfo(Turn.UNDERPASS, 250.0, "u"), 40f)!!.contains("물"))
    }

    @Test fun `오래 달렸을 때만 휴게소 들르자고`() {
        val c = DriveCoach()
        assertNull(c.onTurn(TurnInfo(Turn.REST_AREA, 1200.0, "r"), 80f))
        c.onDrivingMinutes(55, emptySet())
        assertTrue(c.onTurn(TurnInfo(Turn.REST_AREA, 1100.0, "r2"), 80f)!!.contains("55분째"))
    }

    @Test fun `상황 종류 매핑`() {
        assertEquals(SituationKind.ROUNDABOUT, TurnInfo(Turn.ROUNDABOUT, 1.0).situationKind())
        assertEquals(SituationKind.SCHOOL, Caution.SCHOOL_ZONE.situationKind())
        assertNull(Caution.OTHER.situationKind())
    }
}
