package com.yikihyub.drivingpromise.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import com.yikihyub.drivingpromise.domain.CarStatus
import com.yikihyub.drivingpromise.domain.Gear
import com.yikihyub.drivingpromise.domain.Level
import com.yikihyub.drivingpromise.domain.Signal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Pleos Vehicle SDK 래퍼. 이 앱이 보는 차량 신호:
 *  - Brake       : 브레이크 페달 밟힘 → 페달 확인 안내
 *  - Gear        : P/R/N/D → 후진·정차 판단, 운행 시작/종료
 *  - TurnSignal  : 방향지시등 도우미
 *  - SteeringWheel: 회전이 끝났는지(조향각) 판단
 *  - Wiper       : 비 오면 차분 모드
 *  - Light       : 하향등 → 해질녘·비 올 때 전조등 확인
 *  - Odometer    : 연속 주행시간(쉬어 가기), 누적 주행거리(이달의 약속)
 *  - Safety      : 엔진오일·브레이크액·경고등 → 오늘의 차 상태
 *  - Fuel / EvBattery : 연료·배터리
 *
 * Gear API는 2.3.2에서 deprecated 표시지만 정상 동작하고, 대체인 AAOS GEAR_SELECTION 은 privileged 권한이 필요하다.
 * 지원하지 않는 신호는 null 로 남고, 해당 안내만 꺼진다.
 */
@Suppress("DEPRECATION")
class VehicleGateway(context: Context) {

    private val tag = "VehicleGateway"
    private val vehicle = Vehicle(context.applicationContext)
    private val front = WindowArea(WindowValue.FRONT_WINDSHIELD)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _brake = MutableStateFlow<Boolean?>(null)
    val brake: StateFlow<Boolean?> = _brake.asStateFlow()

    private val _gear = MutableStateFlow(Gear.UNKNOWN)
    val gear: StateFlow<Gear> = _gear.asStateFlow()

    private val _signal = MutableStateFlow(Signal.OFF)
    val signal: StateFlow<Signal> = _signal.asStateFlow()

    private val _steeringDeg = MutableStateFlow(0f)
    val steeringDeg: StateFlow<Float> = _steeringDeg.asStateFlow()

    private val _wiperOn = MutableStateFlow<Boolean?>(null)
    val wiperOn: StateFlow<Boolean?> = _wiperOn.asStateFlow()

    private val _lowBeamOn = MutableStateFlow<Boolean?>(null)
    val lowBeamOn: StateFlow<Boolean?> = _lowBeamOn.asStateFlow()

    private val _drivingMinutes = MutableStateFlow<Int?>(null)
    val drivingMinutes: StateFlow<Int?> = _drivingMinutes.asStateFlow()

    private val _odometerKm = MutableStateFlow<Float?>(null)
    val odometerKm: StateFlow<Float?> = _odometerKm.asStateFlow()

    private val _fuel = MutableStateFlow<Int?>(null)
    private val _soc = MutableStateFlow<Int?>(null)

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) { _brake.value = status }
        override fun onFailed(e: Exception) = fail("brake", e)
    }
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(selection: VehicleGear?) { _gear.value = selection.toGear() }
        override fun onFailed(e: Exception) = fail("gear", e)
    }
    private val signalListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) { _signal.value = state.toSignal() }
        override fun onFailed(e: Exception) = fail("turnSignal", e)
    }
    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) { angle?.let { _steeringDeg.value = it } }
        override fun onFailed(e: Exception) = fail("steering", e)
    }
    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) { _wiperOn.value = state?.let { it == WiperState.ON } }
        override fun onFailed(e: Exception) = fail("wiper", e)
    }
    private val lowBeamListener = object : LightStateListener {
        override fun onLightStateUpdated(state: LightState?) { _lowBeamOn.value = state?.let { it == LightState.ON } }
        override fun onFailed(e: Exception) = fail("lowBeam", e)
    }
    private val drivingTimeListener = object : DrivingTimeListener {
        override fun onDrivingTimeUpdated(value: Int?) { value?.let { _drivingMinutes.value = it } }
        override fun onFailed(e: Exception) = fail("drivingTime", e)
    }
    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) { value?.let { _odometerKm.value = it } }
        override fun onFailed(e: Exception) = fail("odometer", e)
    }
    private val fuelListener = object : FuelPercentListener {
        override fun onFuelPercentUpdated(value: Int?) { value?.let { _fuel.value = it } }
        override fun onFailed(e: Exception) = fail("fuel", e)
    }
    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) { value?.let { _soc.value = it } }
        override fun onFailed(e: Exception) = fail("evSoc", e)
    }

    fun start() {
        runCatching { vehicle.initialize() }.onFailure { _connected.value = false; fail("initialize", it); return }
        _connected.value = true
        // 도메인마다 따로 감싸서 한 신호가 없어도 나머지는 동작하게 한다.
        reg("brake") { vehicle.getBrake().registerBrakePedalStatus(brakeListener) }
        reg("gear") { vehicle.getGear().registerGearSelection(gearListener) }
        reg("turnSignal") { vehicle.getTurnSignal().registerTurnSignalState(signalListener) }
        reg("steering") { vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener) }
        reg("wiper") { vehicle.getWiper().registerWiperState(front, wiperListener) }
        reg("lowBeam") { vehicle.getLight().registerLowBeamLightState(lowBeamListener) }
        reg("drivingTime") { vehicle.getOdometer().registerTripCurrentDrivingTime(drivingTimeListener) }
        reg("odometer") { vehicle.getOdometer().registerDistanceDriven(odometerListener) }
        reg("fuel") { vehicle.getFuel().registerFuelPercent(fuelListener) }
        reg("evSoc") { vehicle.getEvBattery().registerEvBatteryStateOfCharge(socListener) }
        primeInitialValues()
    }

    /** 리스너는 "변화"만 알려주므로 초기값은 한 번 직접 읽어둔다. */
    private fun primeInitialValues() {
        reg("brake.get") { vehicle.getBrake().getCurrentBrakePedalStatus({ _brake.value = it }, { fail("brake.get", it) }) }
        reg("gear.get") { vehicle.getGear().getCurrentGearSelectionState({ _gear.value = it.toGear() }, { fail("gear.get", it) }) }
        reg("signal.get") { vehicle.getTurnSignal().getCurrentTurnSignalState({ _signal.value = it.toSignal() }, { fail("signal.get", it) }) }
        reg("wiper.get") { vehicle.getWiper().getCurrentWiperState(front, { s -> _wiperOn.value = s?.let { it == WiperState.ON } }, { fail("wiper.get", it) }) }
        reg("lowBeam.get") { vehicle.getLight().getCurrentLowBeamLightState({ s -> _lowBeamOn.value = s?.let { it == LightState.ON } }, { fail("lowBeam.get", it) }) }
        reg("drivingTime.get") { vehicle.getOdometer().getTripCurrentDrivingTime({ v -> v?.let { _drivingMinutes.value = it } }, { fail("drivingTime.get", it) }) }
        reg("odometer.get") { vehicle.getOdometer().getCurrentDistanceDriven({ v -> v?.let { _odometerKm.value = it } }, { fail("odometer.get", it) }) }
        reg("fuel.get") { vehicle.getFuel().getFuelPercent({ v -> v?.let { _fuel.value = it } }, { fail("fuel.get", it) }) }
        reg("evSoc.get") { vehicle.getEvBattery().getEvBatteryStateOfCharge({ v -> v?.let { _soc.value = it } }, { fail("evSoc.get", it) }) }
    }

    /**
     * 출발 전 "오늘의 차 상태 한마디"에 쓸 값을 모은다. 응답 없는 항목은 1.5초 뒤 모름으로 둔다.
     * 경고등(Telltale)은 privileged 권한이라 일반 앱에서는 보통 실패하고, 그러면 빈 목록이다.
     */
    suspend fun readCarStatus(): CarStatus {
        if (!_connected.value) return CarStatus()
        val oil = one { ok, err -> vehicle.getSafety().getEngineOilLevelState({ ok(it.toLevel()) }, err) } ?: Level.UNKNOWN
        val fluid = one { ok, err -> vehicle.getSafety().getBrakeFluidLevelState({ ok(it.toLevel()) }, err) } ?: Level.UNKNOWN
        val tell = one<Telltales?> { ok, err -> vehicle.getSafety().getTelltalesInfo({ ok(it) }, err) }
        val on = tell?.telltales.orEmpty().filter { it.state == TelltaleState.ON || it.state == TelltaleState.BLINK }.map { it.type.name.lowercase() }
        val fuel = one<Int?> { ok, err -> vehicle.getFuel().getFuelPercent({ ok(it) }, err) } ?: _fuel.value
        val soc = one<Int?> { ok, err -> vehicle.getEvBattery().getEvBatteryStateOfCharge({ ok(it) }, err) } ?: _soc.value
        return CarStatus(
            engineOil = oil, brakeFluid = fluid,
            fuelPercent = fuel.takeIf { soc == null || soc <= 0 },
            evSocPercent = soc?.takeIf { it > 0 },
            yellowTelltales = on,
        )
    }

    fun release() {
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(signalListener) }
        runCatching { vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
        runCatching { vehicle.getWiper().unregisterWiperState(front, wiperListener) }
        runCatching { vehicle.getLight().unregisterLowBeamLightState(lowBeamListener) }
        runCatching { vehicle.getOdometer().unregisterTripCurrentDrivingTime(drivingTimeListener) }
        runCatching { vehicle.getOdometer().unregisterDistanceDriven(odometerListener) }
        runCatching { vehicle.getFuel().unregisterFuelPercent(fuelListener) }
        runCatching { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener) }
        runCatching { vehicle.release() }
        _connected.value = false
    }

    private suspend fun <T> one(block: (ok: (T) -> Unit, err: (Exception) -> Unit) -> Unit): T? =
        withTimeoutOrNull(1_500) {
            suspendCancellableCoroutine<T?> { cont ->
                runCatching {
                    // 차 상태 항목은 차종·권한에 따라 원래 없는 경우가 많다(경고등은 privileged). 오류로 띄우지 않고 로그만.
                    block({ v -> if (cont.isActive) cont.resume(v) }, { e -> Log.i(tag, "status read: ${e.message}"); if (cont.isActive) cont.resume(null) })
                }.onFailure { Log.i(tag, "status read: ${it.message}"); if (cont.isActive) cont.resume(null) }
            }
        }

    private inline fun reg(what: String, block: () -> Unit) { runCatching(block).onFailure { fail(what, it) } }

    /** 차종에 없는 신호(전기차의 연료 등)는 정상이라 화면 오류로 올리지 않는다. 그 신호를 쓰는 안내만 꺼진다. */
    private fun fail(what: String, e: Throwable) {
        val msg = e.message.orEmpty()
        if (msg.contains("not supported", ignoreCase = true) || msg.contains("NOT_SUPPORTED")) {
            Log.i(tag, "$what unsupported on this vehicle")
            return
        }
        Log.w(tag, "$what failed: $msg")
        _lastError.value = "$what: ${msg.ifBlank { e.javaClass.simpleName }}"
    }

    private fun VehicleGear?.toGear() = when (this) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        null, VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE
    }

    private fun TurnSignalValue?.toSignal() = when (this) {
        TurnSignalValue.LEFT -> Signal.LEFT
        TurnSignalValue.RIGHT -> Signal.RIGHT
        else -> Signal.OFF
    }

    private fun OilLevelState?.toLevel() = when (this) {
        OilLevelState.NORMAL -> Level.NORMAL
        OilLevelState.HIGH -> Level.HIGH
        OilLevelState.LOW -> Level.LOW
        OilLevelState.CRITICALLY_LOW -> Level.CRITICALLY_LOW
        OilLevelState.ERROR -> Level.ERROR
        null -> Level.UNKNOWN
    }
}
