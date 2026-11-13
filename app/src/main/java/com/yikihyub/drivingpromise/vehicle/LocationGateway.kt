package com.yikihyub.drivingpromise.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import com.yikihyub.drivingpromise.domain.SurpriseSpot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Pleos FusedLocation SDK 래퍼.
 *  - 현재 위치 → 일몰 계산, 깜짝 지점 저장, 차속 서비스가 없을 때 속도 추정
 *  - addProximityAlert → 깜짝 지점에 가까워지면 미리 알림
 * FUSED_LOCATION 권한은 dangerous 라서 Activity에서 런타임 요청 후 [start]를 불러야 한다.
 */
class LocationGateway(context: Context) {

    private val tag = "LocationGateway"
    private val fused = FusedLocation(context.applicationContext)

    private val _supported = MutableStateFlow(false)
    val supported: StateFlow<Boolean> = _supported.asStateFlow()

    private val _last = MutableStateFlow<LocationData?>(null)
    val last: StateFlow<LocationData?> = _last.asStateFlow()

    private var started = false

    /** 위치 변화로 계산한 속도(km/h). SpeedGateway 가 차속이 없을 때 쓴다 */
    var onSpeed: ((Float, Long) -> Unit)? = null

    /** 등록된 근접 알림 id → 지점 */
    private val alerts = mutableMapOf<String, SurpriseSpot>()

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            location ?: return
            _last.value?.let { prev ->
                val dt = (location.timestampMillis - prev.timestampMillis) / 1000.0
                if (dt in 0.2..5.0) {
                    val m = SurpriseSpot.distanceM(prev.latitude, prev.longitude, location.latitude, location.longitude)
                    onSpeed?.invoke((m / dt * 3.6).toFloat(), location.timestampMillis)
                }
            }
            _last.value = location
        }
        override fun onError(exception: Exception) { Log.w(tag, "location error: ${exception.message}") }
    }

    fun start() {
        if (started) return
        runCatching {
            _supported.value = fused.isSupported()
            if (!_supported.value) return
            fused.initialize()
            fused.registerFusedLocationCallback(listener)
            started = true
        }.onFailure { Log.w(tag, "start failed: ${it.message}") }
    }

    suspend fun current(): LocationData? {
        if (!started) return _last.value
        return suspendCancellableCoroutine { cont ->
            runCatching {
                fused.getLastKnownLocation(object : FusedLocationListener {
                    override fun onChanged(location: LocationData?) {
                        location?.let { _last.value = it }
                        if (cont.isActive) cont.resume(location ?: _last.value)
                    }
                    override fun onError(exception: Exception) {
                        if (cont.isActive) cont.resume(_last.value)
                    }
                })
            }.onFailure { if (cont.isActive) cont.resume(_last.value) }
        }
    }

    /** 지점 반경에 들어오면 [onEnter]. 등록 id 는 내부에서 관리한다. */
    fun watch(spot: SurpriseSpot, onEnter: (SurpriseSpot) -> Unit) {
        if (!started) return
        runCatching {
            fused.addProximityAlert(
                ProximityAlertRequest(spot.lat, spot.lng, SurpriseSpot.ALERT_RADIUS_M, setOf(ProximityTransition.ENTER)),
                object : ProximityAlertListener {
                    override fun onRegistered(alertId: String) { alerts[alertId] = spot }
                    override fun onTransition(event: ProximityAlertEvent) {
                        if (event.transition == ProximityTransition.ENTER) alerts[event.alertId]?.let(onEnter)
                    }
                    override fun onError(exception: Exception) { Log.w(tag, "proximity error: ${exception.message}") }
                },
            )
        }.onFailure { Log.w(tag, "addProximityAlert failed: ${it.message}") }
    }

    fun unwatchAll() {
        alerts.keys.toList().forEach { id -> runCatching { fused.removeProximityAlert(id) } }
        alerts.clear()
    }

    val isStarted get() = started

    fun showBlockedPopup() = runCatching { fused.showLocationAccessBlockedPopup() }

    fun release() {
        if (!started) return
        unwatchAll()
        runCatching {
            fused.unregisterFusedLocationCallback(listener)
            fused.release()
        }
        started = false
    }
}
