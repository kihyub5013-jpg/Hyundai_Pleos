package com.yikihyub.drivingpromise

import android.app.Application
import com.yikihyub.drivingpromise.data.Store
import com.yikihyub.drivingpromise.domain.DrivingCompanion
import com.yikihyub.drivingpromise.vehicle.LocationGateway
import com.yikihyub.drivingpromise.vehicle.NaviGateway
import com.yikihyub.drivingpromise.vehicle.SpeechGateway
import com.yikihyub.drivingpromise.vehicle.SpeedGateway
import com.yikihyub.drivingpromise.vehicle.VehicleGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 앱 전역. Pleos SDK 게이트웨이와 운전 동반자 엔진은 앱 생명주기 동안 하나. */
class PromiseApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val store: Store by lazy { Store(this) }
    val vehicle: VehicleGateway by lazy { VehicleGateway(this) }
    val speed: SpeedGateway by lazy { SpeedGateway(this) }
    val location: LocationGateway by lazy { LocationGateway(this) }
    val navi: NaviGateway by lazy { NaviGateway(this) }
    val speech: SpeechGateway by lazy { SpeechGateway(this, BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET) }
    val companion: DrivingCompanion by lazy { DrivingCompanion(appScope, store, vehicle, speed, location, navi, speech) }

    override fun onCreate() {
        super.onCreate()
        vehicle.start()
        speech.start()
        navi.start()
        companion.start()
        // 시동 후 한 번: 오늘의 차 상태 한마디 (TTS 초기화를 조금 기다린다)
        appScope.launch {
            delay(4_000)
            if (store.settings.value.onboarded) companion.departureBriefing(spoken = true, includeCar = true)
        }
    }

    /** 위치·차속 권한을 받은 뒤 Activity 가 부른다 */
    fun onPermissionsGranted(location: Boolean, carSpeed: Boolean) {
        if (location) { this.location.start(); companion.onLocationReady() }
        if (carSpeed) speed.start()
    }

    override fun onTerminate() {
        companion.stop()
        speech.release()
        navi.release()
        location.release()
        speed.release()
        vehicle.release()
        super.onTerminate()
    }
}
