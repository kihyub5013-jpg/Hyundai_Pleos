package com.yikihyub.drivingpromise.ui

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pleos Connect 앱 정책: 주행 중 UX 제한은 플랫폼이 주는 CarUxRestrictions 를 따라야 한다.
 * 제한이 걸리면 목록·키보드 입력을 숨기고 큰 버튼 + 음성만 남긴다.
 * 일반 에뮬레이터처럼 Car 서비스가 없으면 "제한 없음"으로 둔다.
 */
class UxRestrictionsMonitor(context: Context) {

    private val _restricted = MutableStateFlow(false)
    val restricted: StateFlow<Boolean> = _restricted.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { r: CarUxRestrictions ->
        _restricted.value = r.isRequiresDistractionOptimization
    }

    fun start() {
        runCatching {
            val c = Car.createCar(context)
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            m.registerListener(listener)
            m.currentCarUxRestrictions?.let { _restricted.value = it.isRequiresDistractionOptimization }
            car = c
            manager = m
        }.onFailure { Log.i("UxRestrictions", "car service unavailable: ${it.message}") }
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private val context = context.applicationContext
}
