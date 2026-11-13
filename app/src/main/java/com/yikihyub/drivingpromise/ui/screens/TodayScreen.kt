package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.Gear
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.domain.SunsetVerdict
import com.yikihyub.drivingpromise.domain.VoiceCommand
import com.yikihyub.drivingpromise.domain.spoken
import com.yikihyub.drivingpromise.ui.AheadCard
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.MicButton
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.Panel
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.StatePill
import com.yikihyub.drivingpromise.ui.SunTrack
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.gutter
import com.yikihyub.drivingpromise.ui.icon
import com.yikihyub.drivingpromise.ui.theme.Pleos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * "오늘": 차가 먼저 말하고 화면은 확인해 준다.
 * 왼쪽(좁은 창에서는 위) = 차가 방금 한 말과 말하기 버튼. 오른쪽 = 해의 길, 차 상태, 오늘의 약속.
 * 주행 중에도 이 화면만 남는다: 큰 버튼 세 개와 음성.
 */
@Composable
fun TodayScreen(app: PromiseApp, drivingRestricted: Boolean) {
    val compact = LocalCompact.current
    val ahead by app.companion.ahead.collectAsStateLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TodayHeader(app)
        if (compact) {
            Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AheadCard(ahead)
                VoicePanel(app, drivingRestricted)
                SunPanel(app)
                CarPanel(app)
                PromisePanel(app)
            }
        } else {
            Row(Modifier.padding(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    VoicePanel(app, drivingRestricted)
                    PromisePanel(app)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    AheadCard(ahead)
                    SunPanel(app)
                    CarPanel(app)
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun TodayHeader(app: PromiseApp) {
    val vehicleOn by app.vehicle.connected.collectAsStateLifecycle()
    val naviOn by app.navi.connected.collectAsStateLifecycle()
    PageHeader("오늘") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatePill(if (vehicleOn) "차량 연결" else "차량 없음", if (vehicleOn) Pleos.kept else Pleos.ink4)
            StatePill(if (naviOn) "내비 연결" else "내비 없음", if (naviOn) Pleos.kept else Pleos.ink4)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
/** 차가 방금 한 말. 이 앱의 중심. 아래에 지금 페달 확인 상태와 말하기 버튼. */
@Composable
private fun VoicePanel(app: PromiseApp, drivingRestricted: Boolean) {
    val c = app.companion
    val said by c.lastSaid.collectAsStateLifecycle()
    val listening by c.listening.collectAsStateLifecycle()
    val settings by app.store.settings.collectAsStateLifecycle()
    val sensitive by c.sensitive.collectAsStateLifecycle()
    val speed by c.speedKmh.collectAsStateLifecycle()
    val brake by app.vehicle.brake.collectAsStateLifecycle()
    val gear by app.vehicle.gear.collectAsStateLifecycle()
    val trip by c.trip.collectAsStateLifecycle()
    val home by app.navi.home.collectAsStateLifecycle()
    val scope = rememberCoroutineScope()

    Panel(Modifier.fillMaxWidth(), padding = 36.dp) {
        // 지금 페달 확인이 무엇을 보고 있는지 한 줄
        val (stateText, stateColor) = when {
            !settings.pedalGuard -> "페달 확인 꺼짐" to Pleos.ink4
            sensitive && trip != null -> "페달 확인 · 천천히 구간" to Pleos.caution
            trip != null -> "페달 확인 · 주행 중" to Pleos.kept
            else -> "페달 확인 켜짐" to Pleos.kept
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(stateColor))
            Text(stateText, style = MaterialTheme.typography.titleMedium, color = Pleos.ink2)
        }

        Text(
            said?.let { "“$it”" } ?: "“출발 전 확인을 누르면 오늘 차 상태를 말씀드릴게요.”",
            style = MaterialTheme.typography.headlineMedium,
            color = if (said != null) Pleos.ink else Pleos.ink3,
            maxLines = 3,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        val demo by c.demo.collectAsStateLifecycle()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (demo != null) StatePill("시연 중 · ${demo!!.title}", Pleos.info, filled = true)
            else StatePill(gear.word(), Pleos.ink3)
            StatePill(
                when (brake) { true -> "브레이크 밟음"; false -> "브레이크 안 밟음"; null -> "브레이크 신호 없음" },
                if (brake == true) Pleos.info else Pleos.ink4,
            )
            if (speed > 0.5f) StatePill("${speed.toInt()} km/h", Pleos.ink3)
        }

        Spacer(Modifier.height(8.dp))
        MicButton("말로 하기", listening, { c.listenCommand() }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SecondaryButton("집으로", { c.handle(VoiceCommand.GoHome) }, Modifier.weight(1f), enabled = home != null, icon = Icons.Outlined.Home)
            SecondaryButton("출발 전 확인", { scope.launch { c.departureBriefing() } }, Modifier.weight(1f), icon = Icons.Outlined.Refresh)
        }
        if (!drivingRestricted) {
            Text("이렇게 말해 보세요 · 집에 가자 · 얼마나 남았어 · 차 상태 어때", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
        }
    }
}

/** 해 지기 전 도착: 해의 길 위에 지금·일몰·도착을 놓는다. */
@Composable
private fun SunPanel(app: PromiseApp) {
    val c = app.companion
    val briefing by c.briefing.collectAsStateLifecycle()
    val dest by app.navi.destination.collectAsStateLifecycle()
    val settings by app.store.settings.collectAsStateLifecycle()
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = ZonedDateTime.now() } }
    val sunset = briefing?.sunset ?: c.todaySunset()
    val arrival = dest?.let { now.plusSeconds(it.remainTimeS.toLong()) }
    val promised = Promise.NO_NIGHT in settings.promises

    Panel(Modifier.fillMaxWidth()) {
        Text("해 지기 전 도착", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
        val v = briefing?.sunsetVerdict
        val (line, color) = when {
            sunset == null -> "오늘 해 지는 시각을 계산할 수 없어요." to Pleos.ink3
            !now.isBefore(sunset) -> "해가 졌어요. 전조등을 켜고 천천히 가세요." to Pleos.caution
            dest == null -> "해 지는 시각은 ${sunset.spoken()}이에요. 목적지를 정하면 도착 시각과 견줘 드려요." to Pleos.ink2
            v is SunsetVerdict.Late || (arrival != null && arrival.isAfter(sunset.minusMinutes(10))) ->
                "${dest!!.name}에 해 진 뒤 도착해요." to Pleos.caution
            else -> "${dest!!.name}에 해 지기 전에 도착해요." to Pleos.kept
        }
        Text(line, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 3)
        if (sunset != null && now.isBefore(sunset)) {
            SunTrack(now, sunset, arrival, Modifier.padding(top = 8.dp))
        }
        if (!promised) Text("“해 진 뒤 운전 안 함” 약속을 켜면 출발할 때 말로도 알려 드려요.", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
    }
}

/** 오늘의 차 상태: 한 문장 + 항목 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarPanel(app: PromiseApp) {
    val briefing by app.companion.briefing.collectAsStateLifecycle()
    val parkedAt by app.companion.parkedAt.collectAsStateLifecycle()
    Panel(Modifier.fillMaxWidth()) {
        Text("오늘의 차 상태", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
        val car = briefing?.car
        Text(
            car?.speech ?: "출발 전 확인을 누르면 엔진오일·브레이크액·배터리를 한 문장으로 알려 드려요.",
            style = MaterialTheme.typography.bodyLarge,
            color = if (car == null) Pleos.ink3 else Pleos.ink,
            maxLines = 3,
        )
        car?.items?.let { items ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.filter { it.second != "확인 불가" }.forEach { (k, v) ->
                    val ok = v == "정상" || v == "없음" || v.endsWith("%")
                    StatePill("$k $v", if (ok) Pleos.kept else Pleos.caution)
                }
            }
        }
        parkedAt?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.Outlined.LocalParking, null, Modifier.size(32.dp), tint = Pleos.info)
                Text("주차 위치 · $it", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2, maxLines = 2)
            }
        }
    }
}

/** 오늘 지킬 약속: 아이콘 + 이름, 오늘 운행 기준 상태 */
@Composable
private fun PromisePanel(app: PromiseApp) {
    val settings by app.store.settings.collectAsStateLifecycle()
    val trip by app.companion.trip.collectAsStateLifecycle()
    Panel(Modifier.fillMaxWidth()) {
        Text("오늘 지킬 약속", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
        val list = Promise.entries.filter { it in settings.promises }
        if (list.isEmpty()) Text("아직 정한 약속이 없어요. 정차 중에 “내 약속”에서 골라 주세요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink3)
        list.forEach { p ->
            val atRisk = trip?.let {
                when (p) {
                    Promise.NO_NIGHT -> it.night
                    Promise.AVOID_HIGHWAY -> it.highway
                    Promise.RAIN_NEAR_ONLY -> it.rain
                    Promise.REST_HOURLY -> it.longestStintMin >= 60
                    Promise.NIGHT_WITH_COMPANION -> it.night && it.companion != true
                }
            } ?: false
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Pleos.surfaceHigh).padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Icon(p.icon, null, Modifier.size(36.dp), tint = if (atRisk) Pleos.caution else Pleos.ink2)
                Text(p.title, style = MaterialTheme.typography.titleMedium, color = Pleos.ink, modifier = Modifier.weight(1f))
                Text(if (atRisk) "오늘 어려워요" else "지키는 중", style = MaterialTheme.typography.labelMedium, color = if (atRisk) Pleos.caution else Pleos.kept)
            }
        }
    }
}

private fun Gear.word() = when (this) {
    Gear.PARK -> "P 주차"
    Gear.REVERSE -> "R 후진"
    Gear.NEUTRAL -> "N 중립"
    Gear.DRIVE -> "D 주행"
    Gear.UNKNOWN -> "기어 신호 없음"
}
