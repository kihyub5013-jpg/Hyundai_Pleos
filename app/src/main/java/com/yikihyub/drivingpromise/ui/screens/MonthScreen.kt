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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.ListRow
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.Panel
import com.yikihyub.drivingpromise.ui.PleosSwitch
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.gutter
import com.yikihyub.drivingpromise.ui.icon
import com.yikihyub.drivingpromise.ui.theme.Pleos
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * 이달의 약속: 남과 비교하지 않고 내 약속 기준으로만.
 * 약속마다 이번 달 운행을 점 하나씩 늘어놓는다 (초록 = 지킴, 주황 = 못 지킴). 숫자보다 줄이 먼저 읽힌다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthScreen(app: PromiseApp) {
    val trips by app.store.trips.collectAsStateLifecycle()
    val settings by app.store.settings.collectAsStateLifecycle()
    val spots by app.store.spots.collectAsStateLifecycle()
    val report = remember(trips, settings.promises) { app.companion.monthReport() }
    val zone = ZoneId.systemDefault()
    val monthTrips = remember(trips) {
        trips.filter { YearMonth.from(Instant.ofEpochMilli(it.startMs).atZone(zone)) == YearMonth.now(zone) }.sortedBy { it.startMs }
    }
    val fmt = remember { SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA) }
    val compact = LocalCompact.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("${report.month.monthValue}월의 약속") {
            SecondaryButton("읽어 주기", { app.companion.say(report.speech()) }, icon = Icons.AutoMirrored.Outlined.VolumeUp)
        }

        Panel(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
            Text(
                if (report.trips == 0) "이번 달은 아직 운전 기록이 없어요."
                else if (report.promiseTotal == 0) "이번 달 ${report.trips}번 운전했어요."
                else "이번 달 ${report.trips}번 운전했고, 약속 ${report.promiseTotal}번 중 ${report.keptTotal}번 지켰어요.",
                style = MaterialTheme.typography.headlineMedium,
                color = Pleos.ink,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Fact("달린 거리", "%.0f km".format(report.distanceKm))
                Fact("밤 운전", "${report.nightTrips}번")
                Fact("비 오는 날", "${report.rainTrips}번")
                Fact("가장 길게 쉬지 않고", "${report.longestStintMin}분")
                Fact("페달 확인 안내", "${report.pedalAlerts}번")
            }
        }

        SectionTitle("약속별로 보기")
        if (report.scores.isEmpty()) {
            Text("정한 약속이 없어요. “내 약속”에서 골라 주세요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink3, modifier = Modifier.padding(horizontal = gutter))
        }
        Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            report.scores.forEach { s ->
                Panel(Modifier.fillMaxWidth(), padding = 28.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Icon(s.promise.icon, null, Modifier.size(36.dp), tint = Pleos.ink2)
                        Text(s.promise.title, style = MaterialTheme.typography.titleLarge, color = Pleos.ink, modifier = Modifier.weight(1f))
                        Text(
                            if (s.total == 0) "기록 없음" else "${s.total}번 중 ${s.kept}번",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (s.total > 0 && s.kept == s.total) Pleos.kept else Pleos.ink2,
                        )
                    }
                    if (monthTrips.isNotEmpty()) TripDots(monthTrips.map { it.kept(s.promise) })
                }
            }
        }

        SectionTitle("깜짝 지점 · 다음에 가까워지면 미리 알려 드려요")
        if (spots.isEmpty()) {
            Text("아직 없어요. 페달 확인 안내가 울렸거나 급하게 멈춘 곳이 여기에 남아요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink3, modifier = Modifier.padding(horizontal = gutter))
        }
        spots.takeLast(6).reversed().forEach { s ->
            ListRow(s.place.ifBlank { "%.4f, %.4f".format(s.lat, s.lng) }, "${fmt.format(Date(s.timeMs))} · ${s.reason}", leading = Icons.Outlined.Place)
        }

        SectionTitle("가족과 나누기")
        Panel(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("월간 요약을 가족에게 보내기", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
                    Text("직접 켤 때만 보내요. 위치와 운행 상세는 보내지 않아요.", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink3)
                }
                PleosSwitch(settings.familyShare) { app.store.setFamilyShare(it) }
            }
            if (settings.familyShare) {
                // 받는 사람 휴대폰에 뜰 모습 그대로: 말풍선 한 개
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("가족에게 이렇게 보여요", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
                    Box(
                        Modifier
                            .fillMaxWidth(if (compact) 1f else 0.8f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 28.dp))
                            .background(Pleos.surfaceHigh)
                            .padding(horizontal = 28.dp, vertical = 22.dp),
                    ) { Text(report.familySummary(), style = MaterialTheme.typography.bodyLarge, color = Pleos.ink) }
                    Text("보내기는 다음 단계에서 연결해요. 지금은 미리보기예요.", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
    }
}

/** 운행 하나 = 점 하나. 지킴 초록, 못 지킴 주황 테두리 링. 색과 모양을 함께 바꿔 색약도 구분된다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripDots(kept: List<Boolean>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        kept.forEach { k ->
            if (k) Box(Modifier.size(26.dp).clip(CircleShape).background(Pleos.kept))
            else Box(
                Modifier.size(26.dp).clip(CircleShape).background(Pleos.caution).padding(6.dp).clip(CircleShape).background(Pleos.surface),
            )
        }
    }
}
