package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.domain.Sensitivity
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.MicButton
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.Panel
import com.yikihyub.drivingpromise.ui.PleosSwitch
import com.yikihyub.drivingpromise.ui.PrimaryButton
import com.yikihyub.drivingpromise.ui.Segmented
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.gutter
import com.yikihyub.drivingpromise.ui.icon
import com.yikihyub.drivingpromise.ui.theme.Pleos

/**
 * 약속 고르기. 처음 실행하면 이 화면부터. 정차 중에만 열린다.
 * 약속 하나가 큰 카드 하나: 누르면 켜고 끈다. 말로도 정할 수 있다.
 */
@Composable
fun PromisesScreen(app: PromiseApp, firstRun: Boolean, onDone: () -> Unit) {
    val settings by app.store.settings.collectAsStateLifecycle()
    val listening by app.companion.listening.collectAsStateLifecycle()
    val compact = LocalCompact.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader(
            if (firstRun) "내 운전 약속을 정해요" else "내 약속",
            "스스로 정한 조건을 차가 함께 지켜요. 남과 비교하거나 점수를 매기지 않아요.",
        )

        // 약속 카드: 넓은 창 2열, 좁은 창 1열
        val rows = Promise.entries.chunked(if (compact) 1 else 2)
        Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { p -> PromiseCard(p, p in settings.promises, Modifier.weight(1f)) { app.store.togglePromise(p) } }
                    if (row.size == 1 && !compact) Spacer(Modifier.weight(1f))
                }
            }
            MicButton("말로 정하기 · “밤에는 운전 안 하는 약속”", listening, { app.companion.listenCommand() }, Modifier.fillMaxWidth())
        }

        Panel(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("페달 확인 안내", style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
                    Text("브레이크 대신 가속 페달을 밟은 것 같으면 바로 말로 알려요. 차를 제어하지는 않아요.", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink3)
                }
                PleosSwitch(settings.pedalGuard) { app.store.setPedalGuard(it) }
            }
            Spacer(Modifier.height(8.dp))
            Text("얼마나 자주 알려 드릴까요?", style = MaterialTheme.typography.titleMedium, color = if (settings.pedalGuard) Pleos.ink else Pleos.ink4)
            Segmented(Sensitivity.entries, settings.sensitivity, { it.label }, { app.store.setSensitivity(it) }, Modifier.fillMaxWidth(), enabled = settings.pedalGuard)
            Text("자주 울려 번거로우면 “덜 민감”, 주차가 불안하면 “민감”을 고르세요.", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
        }

        if (firstRun) {
            PrimaryButton(
                if (settings.promises.isEmpty()) "약속 없이 시작하기" else "약속 ${settings.promises.size}개로 시작하기",
                { app.store.setOnboarded(); onDone() },
                Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 40.dp),
                tall = true,
            )
        }
        Spacer(Modifier.height(140.dp))
    }
}

/** 약속 카드: 켜지면 파란 테두리와 체크, 아이콘이 채워진 원 안으로 들어간다. */
@Composable
private fun PromiseCard(p: Promise, on: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 168.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Pleos.surface)
            .border(if (on) 3.dp else 1.dp, if (on) Pleos.info else Pleos.line, MaterialTheme.shapes.large)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(if (on) Pleos.info else Pleos.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(p.icon, null, Modifier.size(42.dp), tint = if (on) androidx.compose.ui.graphics.Color.White else Pleos.ink2)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(p.title, style = MaterialTheme.typography.titleLarge, color = Pleos.ink)
            Text(p.detail, style = MaterialTheme.typography.bodySmall, color = Pleos.ink3, maxLines = 2)
        }
        Icon(
            if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            if (on) "켜짐" else "꺼짐",
            Modifier.size(44.dp),
            tint = if (on) Pleos.info else Pleos.ink4,
        )
    }
}
