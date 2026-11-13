package com.yikihyub.drivingpromise.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.ui.screens.PromiseAppUi
import com.yikihyub.drivingpromise.ui.theme.PromiseTheme

class MainActivity : ComponentActivity() {

    private lateinit var ux: UxRestrictionsMonitor
    private val app get() = application as PromiseApp

    private val ask = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onGranted() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ux = UxRestrictionsMonitor(this).also { it.start() }
        ensurePermissions()
        setContent {
            PromiseTheme {
                val restricted by ux.restricted.collectAsStateLifecycle()
                PromiseAppUi(app = app, drivingRestricted = restricted)
            }
        }
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermissions() {
        val needed = PERMISSIONS.filterNot(::granted)
        if (needed.isEmpty()) onGranted() else ask.launch(needed.toTypedArray())
    }

    private fun onGranted() = app.onPermissionsGranted(location = granted(PLEOS_LOCATION), carSpeed = granted(CAR_SPEED))

    override fun onDestroy() {
        ux.stop()
        super.onDestroy()
    }

    companion object {
        const val PLEOS_LOCATION = "pleos.car.permission.FUSED_LOCATION"
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"
        val PERMISSIONS = listOf(PLEOS_LOCATION, CAR_SPEED, android.Manifest.permission.RECORD_AUDIO)
    }
}
