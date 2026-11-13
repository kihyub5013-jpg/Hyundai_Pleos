package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.Gear
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.domain.SunsetVerdict
import com.yikihyub.drivingpromise.domain.VoiceCommand
import com.yikihyub.drivingpromise.domain.spoken
import com.yikihyub.drivingpromise.ui.Block
import com.yikihyub.drivingpromise.ui.Chip
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.MicButton
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.StatusDot
import com.yikihyub.drivingpromise.ui.Tile
import com.yikihyub.drivingpromise.ui.TileRow
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos
import kotlinx.coroutines.launch

/**
 * "오늘": 출발 전 브리핑(차 상태·일몰·경로)과 지금 차 상태. 주행 중에도 이 화면만 남는다.
 * 조작은 큰 버튼 세 개뿐이고, 나머지는 말로 한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(app: PromiseApp, drivingRestricted: Boolean) {
    val c = app.companion
    val scope = rememberCoroutineScope()
    val settings by app.store.settings.collectAsStateLifecycle()
    val briefing by c.briefing.collectAsStateLifecycle()
    val speed by c.speedKmh.collectAsStateLifecycle()
    val sensitive by c.sensitive.collectAsStateLifecycle()
    val brake by app.vehicle.brake.collectAsStateLifecycle()
    val gear by app.vehicle.gear.collectAsStateLifecycle()
    val vehicleOn by app.vehicle.connected.collectAsStateLifecycle()
    val naviOn by app.navi.connected.collectAsStateLifecycle()
    val dest by app.navi.destination.collectAsStateLifecycle()
    val home by app.navi.home.collectAsStateLifecycle()
    val listening by c.listening.collectAsStateLifecycle()
    val parkedAt by c.parkedAt.collectAsStateLifecycle()
    val trip by c.trip.collectAsStateLifecycle()
    val compact = LocalCompact.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("오늘") {
            StatusDot("차량", vehicleOn)
            StatusDot("내비", naviOn)
        }

        // 지금 차 상태: 페달 확인 안내가 보는 값 그대로
        TileRow(
            tiles = listOf(
                { m -> Tile("속도", "${speed.toInt()} km/h", m) },
                { m -> Tile("브레이크", when (brake) { true -> "밟음"; false -> "안 밟음"; null -> "—" }, m, valueColor = if (brake == true) Pleos.accent else Pleos.ink) },
                { m -> Tile("기어", gear.label(), m) },
                { m -> Tile("페달 안내", if (!settings.pedalGuard) "꺼짐" else if (sensitive) "민감 구간" else "일반 도로", m, valueColor = if (sensitive && settings.pedalGuard) Pleos.warn else Pleos.ink) },
            ),
        )

        Column(Modifier.padding(horizontal = 40.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                MicButton("말로 하기", listening, { c.listenCommand() }, Modifier.weight(1f))
                if (!compact) {
                    SecondaryButton("집으로", { c.handle(VoiceCommand.GoHome) }, Modifier.weight(1f), enabled = home != null, icon = Icons.Outlined.Home, tall = true)
                }
                SecondaryButton("출발 전 확인", { scope.launch { c.departureBriefing() } }, Modifier.weight(1f), icon = Icons.Outlined.Refresh, tall = true)
            }
            if (!drivingRestricted) {
                Text("\"집에 가자\" · \"얼마나 남았어?\" · \"차 상태 어때?\" · \"이번 달 어땠어?\"", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
            }

            // 출발 전 브리핑
            Block(Modifier.fillMaxWidth()) {
                Text("오늘의 차 상태", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
                Text(briefing?.car?.speech ?: "\"출발 전 확인\"을 누르면 차 상태를 한 문장으로 알려드려요.", style = MaterialTheme.typography.titleLarge)
                briefing?.car?.items?.let { items ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { (k, v) -> Chip("$k $v", color = if (v == "정상" || v == "없음" || v.endsWith("%")) null else Pleos.warn) }
                    }
                }
            }

            Block(Modifier.fillMaxWidth()) {
                val sunset = briefing?.sunset ?: c.todaySunset()
                Text("해 지기 전 도착", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
                Text("오늘 해 지는 시각 ${sunset?.spoken() ?: "—"}", style = MaterialTheme.typography.titleLarge)
                val v = briefing?.sunsetVerdict
                when {
                    dest == null -> Text("내비에서 목적지를 정하면 도착 예정 시각을 일몰과 비교해요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink2)
                    v is SunsetVerdict.InTime -> Text("${dest!!.name} · 해 지기 ${v.marginMin}분 전 도착 (${v.arrival.spoken()})", style = MaterialTheme.typography.bodyLarge, color = Pleos.ok)
                    v is SunsetVerdict.Late -> Text("${dest!!.name} · 해 지고 ${v.lateMin}분 뒤 도착 (${v.arrival.spoken()})", style = MaterialTheme.typography.bodyLarge, color = Pleos.danger)
                    v == SunsetVerdict.AlreadyDark -> Text("이미 해가 졌어요. 전조등을 확인하세요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.warn)
                    else -> Text("${dest!!.name} · ${(dest!!.remainTimeS + 59) / 60}분 남음", style = MaterialTheme.typography.bodyLarge)
                }
                briefing?.routePreview?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        }

        SectionTitle("오늘 지킬 약속")
        FlowRow(Modifier.padding(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (settings.promises.isEmpty()) Text("아직 정한 약속이 없어요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink2)
            Promise.entries.filter { it in settings.promises }.forEach { Chip(it.title, selected = true) }
        }

        val status = buildList {
            trip?.let { add("운행 중 · 페달 안내 ${it.pedalAlerts}회" + (if (it.night) " · 밤 운전" else "") + (if (it.rain) " · 비" else "")) }
            parkedAt?.let { add("주차 위치: $it") }
        }
        if (status.isNotEmpty()) {
            SectionTitle("지금")
            status.forEach { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp)) }
        }
        Text(" ", Modifier.padding(bottom = 120.dp))
    }
}

private fun Gear.label() = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R 후진"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
    Gear.UNKNOWN -> "—"
}
