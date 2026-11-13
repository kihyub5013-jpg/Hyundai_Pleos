package com.yikihyub.drivingpromise.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.ui.theme.Pleos

/* Pleos 기본 앱 톤의 공용 조각들. 색·크기는 theme/Theme.kt 의 Pleos 토큰만 쓴다. */

/**
 * 좁은 창 여부. Pleos는 내비와 나란히 두 앱을 띄울 수 있어(Connect-W 2분할) 앱 폭이 절반(약 850px)이 된다.
 * 그때는 메뉴를 아이콘만 남기고, 긴 행은 두 줄로 쌓는다.
 */
val LocalCompact = androidx.compose.runtime.compositionLocalOf { false }

/** 페이지 제목 줄. 오른쪽에 상태 표시 등을 끼울 수 있다. */
@Composable
fun PageHeader(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** 차량 설정의 "도어 · 잠김" 같은 상태 표시. 점 + 라벨. */
@Composable
fun StatusDot(label: String, on: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(12.dp).background(if (on) Pleos.ok else Pleos.ink3, CircleShape))
        if (!LocalCompact.current) Text(label, style = MaterialTheme.typography.bodyMedium, color = if (on) Pleos.ink else Pleos.ink2)
    }
}

/** 얇은 선으로 나뉜 타일 한 칸 (차량 설정의 "모든 창문 / 닫힘"). */
@Composable
fun Tile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Pleos.ink,
    caption: String? = null,
    selected: Boolean = false,
) {
    Column(
        modifier
            .background(if (selected) Pleos.surface else Color.Transparent)
            .padding(horizontal = if (LocalCompact.current) 20.dp else 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
        Text(
            value,
            style = if (LocalCompact.current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = Pleos.ink2)
    }
}

/** 타일을 가로로 늘어놓고 사이에 세로선, 위아래에 가로선. */
@Composable
fun TileRow(modifier: Modifier = Modifier, tiles: List<@Composable (Modifier) -> Unit>) {
    Column(modifier) {
        HorizontalDivider(color = Pleos.line)
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            tiles.forEachIndexed { i, tile ->
                tile(Modifier.weight(1f).fillMaxHeight())
                if (i < tiles.lastIndex) VerticalDivider(color = Pleos.line)
            }
        }
        HorizontalDivider(color = Pleos.line)
    }
}

/** 연한 회색 블록. 안내문·강조 영역. */
@Composable
fun Block(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.background(Pleos.surface, MaterialTheme.shapes.large).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** 검정 주요 버튼 (Pleos basic button). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tall: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (tall) 96.dp else 72.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Pleos.black, contentColor = Color.White,
            disabledContainerColor = Pleos.surface, disabledContentColor = Pleos.ink3,
        ),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(30.dp)); Spacer(Modifier.width(14.dp)) }
        Text(text, style = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge)
    }
}

/** 흰 바탕 테두리 버튼. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tall: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (tall) 96.dp else 72.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, if (enabled) Pleos.ink else Pleos.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Pleos.ink, disabledContentColor = Pleos.ink3),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(30.dp)); Spacer(Modifier.width(14.dp)) }
        Text(text, style = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge)
    }
}

/** 마이크 버튼. 듣는 중이면 파란 테두리로 바뀐다. */
@Composable
fun MicButton(text: String, listening: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = true) {
    if (listening) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(96.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(3.dp, Pleos.accent),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Pleos.accent),
            contentPadding = PaddingValues(horizontal = 32.dp),
        ) {
            Icon(Icons.Filled.Mic, null, Modifier.size(30.dp)); Spacer(Modifier.width(14.dp))
            Text("듣는 중 · 취소", style = MaterialTheme.typography.titleLarge)
        }
    } else if (primary) {
        PrimaryButton(text, onClick, modifier, enabled, Icons.Filled.Mic, tall = true)
    } else {
        SecondaryButton(text, onClick, modifier, enabled, Icons.Filled.Mic, tall = true)
    }
}

/** 세그먼트 메뉴 (차량 설정 전조등 "끄기 | 자동 | …"). 선택 = 검정. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Int = 72,
) {
    Row(
        modifier
            .border(2.dp, Pleos.line, MaterialTheme.shapes.medium)
            .padding(2.dp)
            .height(height.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (sel) Pleos.black else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sel) Color.White else if (enabled) Pleos.ink else Pleos.ink3,
                )
            }
            if (i < options.lastIndex && !sel && options.getOrNull(i + 1) != selected) {
                VerticalDivider(Modifier.padding(vertical = 16.dp), color = Pleos.line)
            }
        }
    }
}

/** 작은 칩 — 결제수단·상태 라벨. 눌러서 고르는 용도면 selected 로 검정. */
@Composable
fun Chip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, color: Color? = null) {
    val bg = if (selected) Pleos.black else Color.Transparent
    val fg = color ?: if (selected) Color.White else Pleos.ink
    Box(
        Modifier
            .border(2.dp, if (selected) Pleos.black else Pleos.line, RoundedCornerShape(50))
            .background(bg, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = fg) }
}

/** 리스트 한 줄: 제목 / 부제 / 오른쪽 값. 아래에 구분선. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Pleos.ink2)
                if (below != null) { Spacer(Modifier.height(6.dp)); below() }
            }
            trailing()
        }
        HorizontalDivider(color = Pleos.line)
    }
}

/** 섹션 제목 (리스트 위 작은 회색 제목). */
@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Pleos.ink2,
        modifier = Modifier.padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 10.dp),
    )
}

/** Pleos 토스트: 화면 아래 가운데, 검정 둥근 상자, 최대 792. 듣는 중엔 파란 마이크. */
@Composable
fun PleosToast(text: String, listening: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = Pleos.black,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.widthIn(max = 792.dp),
    ) {
        Row(Modifier.padding(horizontal = 32.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            if (listening) { Icon(Icons.Filled.Mic, null, Modifier.size(28.dp), tint = Pleos.accent); Spacer(Modifier.width(14.dp)) }
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}

/** Pleos 톤 텍스트필드: 얇은 회색 테두리, 포커스 시 검정. */
@Composable
fun PleosField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier, number: Boolean = false) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Pleos.ink, unfocusedBorderColor = Pleos.line,
            focusedLabelColor = Pleos.ink2, unfocusedLabelColor = Pleos.ink2, cursorColor = Pleos.ink,
        ),
        modifier = modifier,
    )
}
