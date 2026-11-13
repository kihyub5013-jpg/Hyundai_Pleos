package com.yikihyub.drivingpromise.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** 차속 한 점. [source] 는 화면 디버그용 */
data class SpeedReading(val kmh: Float, val timeMs: Long, val source: String)

/**
 * 차속. Pleos Vehicle SDK 에는 차속 도메인이 없어 AAOS 표준 CarPropertyManager(PERF_VEHICLE_SPEED, m/s)를 쓴다.
 * 권한 android.car.permission.CAR_SPEED 는 dangerous 라 런타임 요청 후 [start].
 * Virtual Console 에서 속도를 바꾸면 여기로 바로 들어온다.
 *
 * 차속 서비스를 못 쓰면 [onLocationSpeed] 로 위치 변화에서 계산한 속도를 대신 받는다.
 */
class SpeedGateway(context: Context) {

    private val tag = "SpeedGateway"
    private val appContext = context.applicationContext
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val _speed = MutableStateFlow<SpeedReading?>(null)
    val speed: StateFlow<SpeedReading?> = _speed.asStateFlow()

    private val _carSpeedAvailable = MutableStateFlow(false)
    val carSpeedAvailable: StateFlow<Boolean> = _carSpeedAvailable.asStateFlow()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val ms = (value.value as? Float) ?: return
            _speed.value = SpeedReading(abs(ms) * 3.6f, System.currentTimeMillis(), "car")
        }
        override fun onErrorEvent(propId: Int, zone: Int) { Log.w(tag, "speed error prop=$propId") }
    }

    fun start() {
        if (manager != null) return
        runCatching {
            val c = Car.createCar(appContext)
            val m = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            // 10Hz. 페달 판단은 0.6초 구간 가속도를 보므로 이 정도면 충분하다.
            val ok = m.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, 10f)
            car = c; manager = m
            _carSpeedAvailable.value = ok
            Log.i(tag, "PERF_VEHICLE_SPEED registered=$ok")
        }.onFailure { Log.i(tag, "car speed unavailable: ${it.message}") }
    }

    /** 차속 서비스가 없을 때만 위치 기반 속도를 반영한다. */
    fun onLocationSpeed(kmh: Float, timeMs: Long) {
        if (_carSpeedAvailable.value) return
        _speed.value = SpeedReading(kmh, timeMs, "location")
    }

    /** 시연·테스트용: 화면의 시뮬레이터 버튼이 넣는 값 */
    fun inject(kmh: Float) {
        _speed.value = SpeedReading(kmh, System.currentTimeMillis(), "sim")
    }

    fun release() {
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
        manager = null; car = null
    }
}
