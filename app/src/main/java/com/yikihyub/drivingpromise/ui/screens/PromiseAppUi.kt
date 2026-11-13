package com.yikihyub.drivingpromise.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yikihyub.drivingpromise.PromiseApp
import com.yikihyub.drivingpromise.domain.ActiveAlert
import com.yikihyub.drivingpromise.domain.PedalAlert
import com.yikihyub.drivingpromise.ui.LocalCompact
import com.yikihyub.drivingpromise.ui.PleosToast
import com.yikihyub.drivingpromise.ui.collectAsStateLifecycle
import com.yikihyub.drivingpromise.ui.theme.Pleos
import com.yikihyub.drivingpromise.ui.theme.PleosStatic
import kotlinx.coroutines.delay

enum class Tab(val label: String, val short: String, val icon: ImageVector) {
    TODAY("오늘", "오늘", Icons.Outlined.DirectionsCar),
    PROMISES("내 약속", "약속", Icons.Outlined.Checklist),
    MONTH("이달의 약속", "기록", Icons.Outlined.CalendarMonth),
    SETTINGS("설정·시연", "설정", Icons.Outlined.Tune),
}

/**
 * 루트. Pleos 기본 앱처럼 왼쪽 메뉴 + 오른쪽 내용.
 * 주행 중(UX 제한)에는 "오늘"만 남는다. 페달 확인 안내는 어느 화면에서든 전체를 덮는다.
 * 창 폭 1100dp 미만(2분할 절반·세로 창)이면 메뉴는 아이콘+짧은 글자, 내용은 한 줄로 쌓인다.
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

    // 차가 방금 한 말을 5초 동안 아래에 띄운다 ("오늘" 화면은 본문에 이미 크게 나오므로 생략)
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
                    val t = if (listening) "듣고 있어요" else toast.takeIf { effectiveTab != Tab.TODAY }
                    if (t != null) {
                        PleosToast(t, listening, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp, start = 28.dp, end = 28.dp))
                    }
                }
            }
            AnimatedVisibility(alert != null, enter = fadeIn(tween(120)), exit = fadeOut(tween(300))) {
                alert?.let { PedalOverlay(it) { app.companion.dismissAlert() } }
            }
        }
    }
}

/**
 * 페달 확인 안내. 테마와 무관하게 어두운 판 위에 두 페달을 그린다:
 * 지금 밟은 페달은 빨강으로 채우고, 밟아야 할 브레이크는 초록 테두리로.
 * 음성이 먼저 나가고 화면은 확인용. 누르면 닫힌다.
 */
@Composable
private fun PedalOverlay(a: ActiveAlert, onDismiss: () -> Unit) {
    val compact = LocalCompact.current
    Box(
        Modifier.fillMaxSize().background(PleosStatic.dark).clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        val text: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Text(
                    a.alert.title,
                    color = PleosStatic.light,
                    fontSize = if (compact) 84.sp else 112.sp,
                    lineHeight = if (compact) 92.sp else 120.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(a.alert.speech, color = PleosStatic.light2, style = MaterialTheme.typography.headlineLarge)
                Text("화면을 누르면 닫혀요", color = PleosStatic.light4, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (compact) {
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
                Pedals(a.alert, Modifier.fillMaxWidth().height(320.dp))
                text()
            }
        } else {
            Row(Modifier.padding(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(72.dp)) {
                Pedals(a.alert, Modifier.width(420.dp).height(460.dp))
                Box(Modifier.weight(1f)) { text() }
            }
        }
    }
}

/** 브레이크(넓은 왼쪽)와 가속 페달(좁고 긴 오른쪽). 실제 차의 배치 그대로. */
@Composable
private fun Pedals(alert: PedalAlert, modifier: Modifier) {
    val red = Color(0xFFFF4C28)      // informative_negative (dark)
    val green = Color(0xFF00E074)    // informative_active (dark)
    val orange = Color(0xFFFF8A00)   // regulation_orange
    val idle = Color(0xFF44464E)
    val (brakeFill, brakeEdge, gasFill) = when (alert) {
        PedalAlert.GAS_INSTEAD_OF_BRAKE, PedalAlert.LURCH_FROM_STOP -> Triple(Color.Transparent, green, red)
        PedalAlert.BRAKE_NOT_ENOUGH -> Triple(orange, orange, idle)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            val w = size.width; val h = size.height
            val gap = w * 0.08f
            val brakeW = w * 0.56f
            val gasW = w - brakeW - gap
            // 브레이크: 넓고 낮게
            pedal(Offset(0f, h * 0.35f), Size(brakeW, h * 0.55f), brakeFill, brakeEdge)
            // 가속 페달: 좁고 길게
            pedal(Offset(brakeW + gap, h * 0.05f), Size(gasW, h * 0.9f), gasFill, if (gasFill == idle) idle else gasFill)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (alert == PedalAlert.BRAKE_NOT_ENOUGH) "브레이크 · 끝까지" else "브레이크 · 여기로",
                color = if (alert == PedalAlert.BRAKE_NOT_ENOUGH) orange else green,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(0.6f),
            )
            Text(
                if (alert == PedalAlert.BRAKE_NOT_ENOUGH) "가속" else "가속 · 지금",
                color = if (alert == PedalAlert.BRAKE_NOT_ENOUGH) PleosStatic.light3 else red,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(0.4f),
            )
        }
    }
}

private fun DrawScope.pedal(topLeft: Offset, size: Size, fill: Color, edge: Color) {
    val r = CornerRadius(28.dp.toPx())
    if (fill != Color.Transparent) drawRoundRect(fill, topLeft, size, r)
    drawRoundRect(edge, topLeft, size, r, style = Stroke(8.dp.toPx()))
    // 고무 홈 (가로줄)
    val lines = 5
    val inset = size.width * 0.18f
    for (i in 1..lines) {
        val y = topLeft.y + size.height * i / (lines + 1)
        drawLine(
            if (fill == Color.Transparent) edge.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.22f),
            Offset(topLeft.x + inset, y), Offset(topLeft.x + size.width - inset, y),
            strokeWidth = 6.dp.toPx(),
        )
    }
}

@Composable
private fun SideMenu(selected: Tab, enabled: Boolean, compact: Boolean, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.width(if (compact) 132.dp else 300.dp).fillMaxHeight().background(Pleos.bg).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tab.entries.forEach { t ->
            val sel = t == selected
            val on = enabled || sel
            val fg = if (sel) Pleos.ink else if (on) Pleos.ink2 else Pleos.ink4
            val base = Modifier
                .fillMaxWidth()
                .height(if (compact) 112.dp else 96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (sel) Pleos.surfaceHigh else Color.Transparent)
                .clickable(enabled = on, role = Role.Tab) { onSelect(t) }
            if (compact) {
                Column(base, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(t.icon, null, Modifier.size(36.dp), tint = fg)
                    Spacer(Modifier.height(6.dp))
                    Text(t.short, style = MaterialTheme.typography.bodySmall, color = fg, maxLines = 1)
                }
            } else {
                Row(base.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(t.icon, null, Modifier.size(36.dp), tint = fg)
                    Text(t.label, style = MaterialTheme.typography.titleLarge.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium), color = fg)
                }
            }
        }
    }
}
