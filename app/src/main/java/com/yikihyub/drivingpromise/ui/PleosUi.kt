package com.yikihyub.drivingpromise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.domain.Promise
import com.yikihyub.drivingpromise.ui.theme.Pleos
import com.yikihyub.drivingpromise.ui.theme.PleosStatic

/*
 * Pleos Connect 기본 앱 문법의 공용 조각. 색은 theme/Theme.kt 의 Pleos 토큰만 쓴다.
 * 터치 영역은 최소 88dp 높이 (Android 48dp 기준보다 넉넉하게: 움직이는 차 안, 한 손 조작).
 */

/** 좁은 창(2분할 절반, 세로 창). 이때 메뉴는 아이콘만, 가로 배치는 세로로 쌓는다. */
val LocalCompact = androidx.compose.runtime.compositionLocalOf { false }

/** 화면 여백. 넓은 창 48, 좁은 창 28 */
val gutter: Dp @Composable get() = if (LocalCompact.current) 28.dp else 48.dp

/** 약속마다 같은 아이콘을 앱 전체에서 쓴다 (Pleos: 아이콘 + 글자 라벨, 일관성) */
val Promise.icon: ImageVector
    get() = when (this) {
        Promise.NO_NIGHT -> Icons.Outlined.DarkMode
        Promise.AVOID_HIGHWAY -> Icons.Outlined.AltRoute
        Promise.RAIN_NEAR_ONLY -> Icons.Outlined.Umbrella
        Promise.REST_HOURLY -> Icons.Outlined.LocalCafe
        Promise.NIGHT_WITH_COMPANION -> Icons.Outlined.People
    }

/** 화면 제목. 부제는 한 줄. */
@Composable
fun PageHeader(title: String, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 36.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Pleos.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Pleos.ink3)
        }
        trailing()
    }
}

/** 섹션 제목: 목록·묶음 위의 작은 제목. 위 여백이 아래보다 크다. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Pleos.ink2,
        modifier = modifier.padding(start = gutter, end = gutter, top = 40.dp, bottom = 16.dp),
    )
}

/** 면: Pleos surface_basic 위의 둥근 판. 그림자 레벨 1 (y6 blur24 8%). */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Pleos.surface,
    padding: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .shadow(if (Pleos.c.dark) 0.dp else 12.dp, MaterialTheme.shapes.large, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** 점 + 낱말 상태 표시. 색만으로 뜻을 전하지 않는다. */
@Composable
fun StatePill(label: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = 0.14f) else Pleos.surfaceHigh)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Pleos.ink)
    }
}

/** 버튼 공통: Pleos basic button (Pressed / Disabled 상태) */
@Composable
private fun PleosButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    container: Color,
    pressedContainer: Color,
    content: Color,
    height: Dp,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg = when {
        !enabled -> Pleos.surfaceHigh
        pressed -> pressedContainer
        else -> container
    }
    val fg = if (enabled) content else Pleos.ink4
    Row(
        modifier
            .defaultMinSize(minHeight = height)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(34.dp), tint = fg); Spacer(Modifier.width(14.dp)) }
        Text(text, style = textStyle, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 채운 버튼 (button_filled). 화면당 하나의 주된 동작에만. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, tall: Boolean = false) =
    PleosButtonBase(
        text, onClick, modifier, enabled, icon,
        container = Pleos.filled, pressedContainer = Pleos.filled.copy(alpha = 0.82f), content = Pleos.onFilled,
        height = if (tall) 112.dp else 88.dp,
        textStyle = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
    )

/** 기본 버튼 (button_basic): 옅은 회색 면, 테두리 없음. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, tall: Boolean = false) =
    PleosButtonBase(
        text, onClick, modifier, enabled, icon,
        container = Pleos.surfaceHigh, pressedContainer = Pleos.surfaceAccent, content = Pleos.ink,
        height = if (tall) 112.dp else 88.dp,
        textStyle = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
    )

/** 말하기 버튼. 듣는 중이면 파랑(informative_positive)으로 바뀌고 누르면 취소. */
@Composable
fun MicButton(text: String, listening: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    PleosButtonBase(
        if (listening) "듣고 있어요 · 누르면 취소" else text, onClick, modifier, true, Icons.Filled.Mic,
        container = if (listening) Pleos.info else Pleos.filled,
        pressedContainer = if (listening) Pleos.info.copy(alpha = 0.85f) else Pleos.filled.copy(alpha = 0.82f),
        content = if (listening) Color.White else Pleos.onFilled,
        height = 112.dp, textStyle = MaterialTheme.typography.titleLarge,
    )

/** 세그먼트: 둥근 홈 안에서 선택된 칸만 흰 판으로 떠오른다 (Pleos 차량 설정의 선택 컨트롤). */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier.clip(MaterialTheme.shapes.medium).background(Pleos.surfaceHigh).padding(6.dp).height(88.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { o ->
            val sel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Pleos.surface else Color.Transparent)
                    .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!enabled) Pleos.ink4 else if (sel) Pleos.ink else Pleos.ink3,
                )
            }
        }
    }
}

/** Pleos 톤 스위치: 켜짐 = 초록(informative_active) */
@Composable
fun PleosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Pleos.kept, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = Pleos.surfaceAccent, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** 목록 한 줄: 제목 / 부제 / 오른쪽. 아래에 구분선. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = gutter, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (leading != null) Icon(leading, null, Modifier.size(36.dp), tint = Pleos.ink2)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Pleos.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Pleos.ink3, maxLines = 3)
            }
            trailing()
        }
        HorizontalDivider(Modifier.padding(horizontal = gutter), color = Pleos.line)
    }
}

/** Pleos 토스트: 아래 가운데, 어두운 고정색 판, 최대 폭 800. 차가 방금 한 말을 보여준다. */
@Composable
fun PleosToast(text: String, listening: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .widthIn(max = 800.dp)
            .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x3D000000), spotColor = Color(0x3D000000))
            .clip(RoundedCornerShape(24.dp))
            .background(PleosStatic.dark)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Mic, null, Modifier.size(30.dp), tint = if (listening) Color(0xFF0082FF) else PleosStatic.light3)
        Spacer(Modifier.width(18.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = PleosStatic.light, maxLines = 3)
    }
}
