package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.yikihyub.drivingpromise.ui.PrimaryButton
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos

/** 연결 상태와 시연. 시연은 Virtual Console 없이 페달 확인 안내 흐름을 재현한다 (기록·깜짝 지점에는 남기지 않음). */
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
        PageHeader("설정·시연")

        SectionTitle("시연 (Virtual Console 없이)")
        Column(Modifier.padding(horizontal = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DemoScenario.entries.forEach { s ->
                PrimaryButton(if (demo == s) "${s.title} · 진행 중" else s.title, { app.companion.runDemo(s) }, Modifier.fillMaxWidth(), enabled = demo == null)
            }
            Text("차속·브레이크·기어 값을 앱 안에서 바꿔 페달 확인 안내가 울리는 장면을 보여줘요. 실제 Virtual Console 신호를 써도 같은 판단을 거쳐요.",
                style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
        }

        SectionTitle("연결 상태")
        ListRow("Vehicle SDK", vehicleErr ?: "브레이크·기어·방향지시등·와이퍼·전조등·오일", trailing = { State(vehicleOn) })
        ListRow("차속 (AAOS PERF_VEHICLE_SPEED)", speedReading?.let { "${it.kmh.toInt()} km/h · ${it.source}" } ?: "값 없음", trailing = { State(carSpeed) })
        ListRow("NaviHelper", "목적지·회전 안내·주의구간·경로 옵션", trailing = { State(naviOn) })
        ListRow("TextToSpeech (ON_DEVICE)", null, trailing = { State(ttsOn) })
        ListRow(
            "SpeechToText",
            if (BuildConfig.PLEOS_CLIENT_ID.isBlank()) "ON_DEVICE (.env 없음)" else if (sttServer) "HYBRID · 서버 연결됨" else "HYBRID · 서버 연결 대기 (CRN 확인)",
            trailing = { State(sttOn) },
        )
        ListRow("Pleos 프로젝트", BuildConfig.PLEOS_PROJECT_ID.ifBlank { "—" })

        SectionTitle("기록")
        Column(Modifier.padding(horizontal = 40.dp, vertical = 8.dp)) {
            SecondaryButton("깜짝 지점 ${spots.size}곳 지우기", { app.store.clearSpots() }, enabled = spots.isNotEmpty())
        }
        Text(" ", Modifier.padding(bottom = 120.dp))
    }
}

@Composable
private fun State(on: Boolean) {
    Text(if (on) "연결됨" else "없음", style = MaterialTheme.typography.titleMedium, color = if (on) Pleos.ok else Pleos.ink3)
}
