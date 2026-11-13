package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.BuildConfig
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.DemoScenario
import com.yikihyub.drivingpromise.ui.ListRow
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.Panel
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.StatePill
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.gutter
import com.yikihyub.drivingpromise.ui.theme.Pleos

/** 시연과 연결 상태. 시연 값은 운행 기록·깜짝 지점에 남지 않는다. */
@Composable
fun SettingsScreen(app: PromiseApp) {
    val demo by app.companion.demo.collectAsStateLifecycle()
    val vehicleOn by app.vehicle.connected.collectAsStateLifecycle()
    val vehicleErr by app.vehicle.lastError.collectAsStateLifecycle()
    val naviOn by app.navi.connected.collectAsStateLifecycle()
    val carSpeed by app.speed.carSpeedAvailable.collectAsStateLifecycle()
    val speedReading by app.speed.speed.collectAsStateLifecycle()
    val ttsOn by app.speech.ttsReady.collectAsStateLifecycle()
    val sttOn by app.speech.sttReady.collectAsStateLifecycle()
    val sttServer by app.speech.sttServer.collectAsStateLifecycle()
    val spots by app.store.spots.collectAsStateLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("설정·시연", "Virtual Console 없이도 페달 확인 안내가 울리는 장면을 보여 드려요.")

        Panel(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
            Text("시연", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DemoScenario.entries.forEach { s ->
                    SecondaryButton(
                        if (demo == s) "${s.title} · 진행 중" else s.title,
                        { app.companion.runDemo(s) },
                        Modifier.fillMaxWidth(),
                        enabled = demo == null,
                        icon = Icons.Outlined.PlayArrow,
                    )
                }
            }
            Text("차속·브레이크·기어 값을 앱 안에서 바꿔요. 실제 차 신호와 똑같은 판단을 거쳐요.", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
        }

        SectionTitle("연결 상태")
        ListRow("차량 신호", vehicleErr ?: "브레이크 · 기어 · 방향지시등 · 와이퍼 · 전조등 · 오일", leading = Icons.Outlined.DirectionsCar) { Status(vehicleOn) }
        ListRow("차속", speedReading?.let { "${it.kmh.toInt()} km/h" } ?: "값 없음", leading = Icons.Outlined.Speed) { Status(carSpeed) }
        ListRow("내비게이션", "목적지 · 회전 안내 · 주의구간 · 경로 옵션", leading = Icons.Outlined.Navigation) { Status(naviOn) }
        ListRow("음성 안내", "차 안에서 바로 읽어요 (네트워크 없이)", leading = Icons.Outlined.GraphicEq) { Status(ttsOn) }
        ListRow(
            "음성 인식",
            if (BuildConfig.PLEOS_CLIENT_ID.isBlank()) "차 안 인식만" else if (sttServer) "서버 인식 연결됨" else "서버 연결 대기",
            leading = Icons.Outlined.Mic,
        ) { Status(sttOn) }

        SectionTitle("기록")
        ListRow("깜짝 지점 ${spots.size}곳", "페달 확인 안내가 울렸거나 급하게 멈춘 곳", leading = Icons.Outlined.DeleteOutline) {
            SecondaryButton("지우기", { app.store.clearSpots() }, enabled = spots.isNotEmpty())
        }
        Text(
            "Pleos 프로젝트 ${BuildConfig.PLEOS_PROJECT_ID.ifBlank { "—" }} · 버전 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall, color = Pleos.ink4,
            modifier = Modifier.padding(horizontal = gutter, vertical = 28.dp),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun Status(on: Boolean) = StatePill(if (on) "연결됨" else "없음", if (on) Pleos.kept else Pleos.ink4)
