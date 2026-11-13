package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.domain.Sensitivity
import com.yikihyub.drivingpromise.ui.ListRow
import com.yikihyub.drivingpromise.ui.MicButton
import com.yikihyub.drivingpromise.ui.PageHeader
import com.yikihyub.drivingpromise.ui.PrimaryButton
import com.yikihyub.drivingpromise.ui.SectionTitle
import com.yikihyub.drivingpromise.ui.Segmented
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos

/** 약속 고르기. 처음 실행 때는 이 화면부터. 말로도 정할 수 있다 ("밤에는 운전 안 할래"). 정차 중에만 열린다. */
@Composable
fun PromisesScreen(app: PromiseApp, firstRun: Boolean, onDone: () -> Unit) {
    val settings by app.store.settings.collectAsStateLifecycle()
    val listening by app.companion.listening.collectAsStateLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader(if (firstRun) "내 운전 약속 정하기" else "내 약속")
        Text(
            "운전을 그만두게 하는 앱이 아니에요. 스스로 정한 조건을 차가 함께 지켜드려요.\n남과 비교하지 않고, 점수를 매기지 않아요.",
            style = MaterialTheme.typography.bodyLarge, color = Pleos.ink2,
            modifier = Modifier.padding(horizontal = 40.dp),
        )

        SectionTitle("약속 (여러 개 고를 수 있어요)")
        Promise.entries.forEach { p ->
            ListRow(p.title, p.detail, trailing = {
                Switch(
                    checked = p in settings.promises,
                    onCheckedChange = { app.store.togglePromise(p) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pleos.black),
                )
            })
        }

        Column(Modifier.padding(horizontal = 40.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MicButton("말로 정하기", listening, { app.companion.listenCommand() }, Modifier.fillMaxWidth(), primary = false)
            Text("예: \"밤에는 운전 안 하는 약속\", \"고속도로 피하는 약속\"", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
        }

        SectionTitle("페달 확인 안내")
        ListRow("페달 확인 안내", "브레이크 대신 가속 페달을 밟은 것 같으면 바로 음성으로 알려요. 차를 제어하지 않아요.", trailing = {
            Switch(settings.pedalGuard, { app.store.setPedalGuard(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Pleos.black))
        })
        Column(Modifier.padding(horizontal = 40.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("민감도", style = MaterialTheme.typography.titleMedium)
            Segmented(Sensitivity.entries, settings.sensitivity, { it.label }, { app.store.setSensitivity(it) }, Modifier.fillMaxWidth(), enabled = settings.pedalGuard)
            Text("자주 울리면 \"덜 민감\", 주차가 불안하면 \"민감\"을 고르세요.", style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
        }

        if (firstRun) {
            Row(Modifier.padding(40.dp)) {
                PrimaryButton("이대로 시작하기", { app.store.setOnboarded(); onDone() }, Modifier.fillMaxWidth(), tall = true)
            }
        }
        Text(" ", Modifier.padding(bottom = 120.dp))
    }
}
