package com.yikihyub.drivingpromise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PedalGuardTest {

    /** [from]→[to] km/h 를 [ms] 동안 100ms 간격으로 넣고 처음 울린 안내를 돌려준다 */
    private fun PedalGuard.ramp(t0: Long, from: Float, to: Float, ms: Long, brake: Boolean, gear: Gear = Gear.DRIVE, nearParking: Boolean = false): Pair<PedalAlert?, Long> {
        var first: PedalAlert? = null
        val steps = (ms / 100).toInt()
        for (i in 0..steps) {
            val a = onSample(PedalSample(t0 + i * 100, from + (to - from) * i / steps, brake, gear, nearParking))
            if (first == null) first = a
        }
        return first to t0 + steps * 100
    }

    @Test fun `후진 중 브레이크 없이 속도가 빠르게 오르면 엑셀 안내`() {
        val g = PedalGuard()
        val (_, t) = g.ramp(0, 0f, 0f, 1000, brake = true, gear = Gear.REVERSE)
        val (a, _) = g.ramp(t + 2000, 2f, 16f, 1200, brake = false, gear = Gear.REVERSE)
        assertEquals(PedalAlert.GAS_INSTEAD_OF_BRAKE, a)
    }

    @Test fun `보통 출발은 울리지 않는다`() {
        val g = PedalGuard()
        val (_, t) = g.ramp(0, 0f, 0f, 1000, brake = true)
        // 0→20km/h 를 4초에 (5km/h/s)
        val (a, _) = g.ramp(t, 0f, 20f, 4000, brake = false)
        assertNull(a)
    }

    @Test fun `정차 직후 급출발은 발 확인 안내`() {
        val g = PedalGuard()
        val (_, t) = g.ramp(0, 0f, 0f, 1000, brake = false)
        val (a, _) = g.ramp(t + 100, 0f, 15f, 1000, brake = false)
        assertEquals(PedalAlert.LURCH_FROM_STOP, a)
    }

    @Test fun `브레이크를 밟았는데 속도가 오르면 끝까지 밟으라고`() {
        val g = PedalGuard()
        val (_, t) = g.ramp(0, 25f, 25f, 2000, brake = false)
        val (a, _) = g.ramp(t + 100, 25f, 34f, 2500, brake = true)
        assertEquals(PedalAlert.BRAKE_NOT_ENOUGH, a)
    }

    @Test fun `브레이크로 감속하면 조용하다`() {
        val g = PedalGuard()
        val (a, _) = g.ramp(0, 40f, 0f, 4000, brake = true)
        assertNull(a)
    }

    @Test fun `고속도로 합류 가속은 일반 도로에서 울리지 않는다`() {
        val g = PedalGuard()
        // 60→100km/h 를 4초에 (10km/h/s) — 민감 구간 기준이면 울리지만 일반 도로라 넘어간다
        val (a, _) = g.ramp(0, 60f, 100f, 4000, brake = false)
        assertNull(a)
    }

    @Test fun `일반 도로라도 극단적인 가속은 울린다`() {
        val g = PedalGuard()
        val (a, _) = g.ramp(0, 40f, 80f, 2000, brake = false)
        assertEquals(PedalAlert.GAS_INSTEAD_OF_BRAKE, a)
    }

    @Test fun `같은 안내는 쿨다운 동안 한 번만`() {
        val g = PedalGuard()
        var count = 0
        for (i in 0..30) {
            if (g.onSample(PedalSample(i * 100L, 5f + i * 1.5f, false, Gear.REVERSE)) != null) count++
        }
        assertEquals(1, count)
    }

    @Test fun `덜 민감하게 하면 경계 가속은 넘어간다`() {
        val g = PedalGuard(Sensitivity.LOW)
        // 5→15 를 1초에 (10km/h/s): 보통이면 울리지만 덜 민감(×1.35=12.15)이면 조용
        val (a, _) = g.ramp(0, 5f, 15f, 1000, brake = false, gear = Gear.REVERSE)
        assertNull(a)
        val n = PedalGuard(Sensitivity.NORMAL)
        assertEquals(PedalAlert.GAS_INSTEAD_OF_BRAKE, n.ramp(0, 5f, 15f, 1000, brake = false, gear = Gear.REVERSE).first)
    }

    @Test fun `P 에서는 판단하지 않는다`() {
        val g = PedalGuard()
        val (a, _) = g.ramp(0, 0f, 30f, 1000, brake = false, gear = Gear.PARK)
        assertNull(a)
    }
}
