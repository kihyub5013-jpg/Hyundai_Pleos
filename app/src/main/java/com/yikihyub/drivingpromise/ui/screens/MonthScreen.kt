package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.ui.Block
import com.yikihyub.drivingpromise.ui.ListRow
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.SecondaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.Tile
import com.yikihyub.drivingpromise.ui.TileRow
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 이달의 약속: 남과 비교하지 않고 내 약속 기준으로만. 가족 공유는 본인이 켤 때만. */
@Composable
fun MonthScreen(app: PromiseApp) {
    val trips by app.store.trips.collectAsStateLifecycle()
    val settings by app.store.settings.collectAsStateLifecycle()
    val spots by app.store.spots.collectAsStateLifecycle()
    val report = remember(trips, settings.promises) { app.companion.monthReport() }
    val fmt = remember { SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("${report.month.monthValue}월의 약속") {
            SecondaryButton("읽어 주기", { app.companion.say(report.speech()) }, icon = Icons.Outlined.VolumeUp)
        }
        TileRow(
            tiles = listOf(
                { m -> Tile("운전", "${report.trips}번", m, caption = "%.0f km".format(report.distanceKm)) },
                { m -> Tile("약속 지킴", if (report.promiseTotal == 0) "—" else "${report.keptTotal} / ${report.promiseTotal}", m) },
                { m -> Tile("밤 운전", "${report.nightTrips}번", m) },
            ),
        )
        TileRow(
            tiles = listOf(
                { m -> Tile("비 오는 날", "${report.rainTrips}번", m) },
                { m -> Tile("가장 긴 연속 운전", "${report.longestStintMin}분", m) },
                { m -> Tile("페달 확인 안내", "${report.pedalAlerts}회", m) },
            ),
        )

        SectionTitle("약속별")
        if (report.scores.isEmpty()) {
            Text("정한 약속이 없어요. \"내 약속\"에서 골라 주세요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink2, modifier = Modifier.padding(horizontal = 40.dp))
        }
        report.scores.forEach { s ->
            ListRow(s.promise.title, trailing = { Text(if (s.total == 0) "기록 없음" else "${s.total}번 중 ${s.kept}번 지킴", style = MaterialTheme.typography.titleMedium) })
        }

        SectionTitle("깜짝 지점 (다음에 가까워지면 미리 알려드려요)")
        if (spots.isEmpty()) {
            Text("아직 없어요.", style = MaterialTheme.typography.bodyLarge, color = Pleos.ink2, modifier = Modifier.padding(horizontal = 40.dp))
        }
        spots.takeLast(10).reversed().forEach { s ->
            ListRow(s.place.ifBlank { "%.5f, %.5f".format(s.lat, s.lng) }, "${fmt.format(Date(s.timeMs))} · ${s.reason}")
        }

        SectionTitle("가족 공유")
        ListRow("가족에게 월간 요약 보내기", "본인이 켤 때만 보내요. 위치와 운행 상세는 보내지 않아요.", trailing = {
            Switch(settings.familyShare, { app.store.setFamilyShare(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Pleos.black))
        })
        if (settings.familyShare) {
            Column(Modifier.padding(horizontal = 40.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Block(Modifier.fillMaxWidth()) {
                    Text("보낼 내용 미리보기", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
                    Text(report.familySummary(), style = MaterialTheme.typography.titleLarge)
                }
                Text("전송 서버는 다음 단계에서 연결해요. 지금은 미리보기만 보여요.", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
            }
        }
        Text(" ", Modifier.padding(bottom = 120.dp))
    }
}
