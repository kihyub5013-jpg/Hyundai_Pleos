package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.ActiveAlert
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.PleosToast
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("오늘", Icons.Outlined.DirectionsCar),
    PROMISES("내 약속", Icons.Outlined.Checklist),
    MONTH("이달의 약속", Icons.Outlined.CalendarMonth),
    SETTINGS("설정·시연", Icons.Outlined.Settings),
}

/**
 * 루트. Pleos 기본 앱과 같은 왼쪽 메뉴 + 오른쪽 내용.
 * 주행 중(UX 제한)에는 "오늘" 화면만 남긴다: 큰 버튼과 음성으로만 쓴다.
 * 페달 확인 안내는 어떤 화면에서든 전체 화면으로 덮는다.
 */
@Composable
fun PromiseAppUi(app: PromiseApp, drivingRestricted: Boolean) {
    val settings by app.store.settings.collectAsStateLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val effectiveTab = when {
        !settings.onboarded -> Tab.PROMISES
        drivingRestricted -> Tab.TODAY
        else -> tab
    }
    val alert by app.companion.alert.collectAsStateLifecycle()
    val said by app.companion.lastSaid.collectAsStateLifecycle()
    val listening by app.companion.listening.collectAsStateLifecycle()

    // 마지막 안내 문장을 5초 동안 토스트로
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(said) { toast = said; delay(5_000); if (toast == said) toast = null }

    BoxWithConstraints(Modifier.fillMaxSize().background(Pleos.bg)) {
        val compact = maxWidth < 1100.dp
        CompositionLocalProvider(LocalCompact provides compact) {
            Row(Modifier.fillMaxSize()) {
                SideMenu(effectiveTab, enabled = settings.onboarded && !drivingRestricted, compact) { tab = it }
                VerticalDivider(color = Pleos.line)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (effectiveTab) {
                        Tab.TODAY -> TodayScreen(app, drivingRestricted)
                        Tab.PROMISES -> PromisesScreen(app, firstRun = !settings.onboarded) { tab = Tab.TODAY }
                        Tab.MONTH -> MonthScreen(app)
                        Tab.SETTINGS -> SettingsScreen(app)
                    }
                    val t = if (listening) "듣고 있어요" else toast
                    if (t != null) {
                        PleosToast(t, listening, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 24.dp, end = 24.dp))
                    }
                }
            }
            alert?.let { PedalOverlay(it) { app.companion.dismissAlert() } }
        }
    }
}

/** 페달 확인 안내: 화면 전체를 덮는 큰 글씨. 음성이 먼저고 화면은 보조. 누르면 닫힌다. */
@Composable
private fun PedalOverlay(a: ActiveAlert, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Pleos.danger).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Text(a.alert.title, color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 130.sp)
            Text(a.alert.speech, color = Color.White, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SideMenu(selected: Tab, enabled: Boolean, compact: Boolean, onSelect: (Tab) -> Unit) {
    Column(Modifier.width(if (compact) 120.dp else 320.dp).fillMaxHeight().background(Pleos.bg)) {
        Spacer(Modifier.height(8.dp))
        Tab.entries.forEach { t ->
            val sel = t == selected
            val on = enabled || sel
            val fg = if (sel) Color.White else if (on) Pleos.ink else Pleos.ink3
            val base = Modifier.fillMaxWidth().height(96.dp).background(if (sel) Pleos.black else Color.Transparent).clickable(enabled = on) { onSelect(t) }
            if (compact) {
                Column(base, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(t.icon, t.label, Modifier.size(32.dp), tint = fg)
                    Text(t.label, style = MaterialTheme.typography.bodySmall, color = fg)
                }
            } else {
                Row(base.padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(t.icon, t.label, Modifier.size(34.dp), tint = fg)
                    Text(t.label, style = MaterialTheme.typography.titleLarge, color = fg)
                }
            }
        }
    }
}
