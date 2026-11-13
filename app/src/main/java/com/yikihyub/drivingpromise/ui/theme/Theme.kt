package com.yikihyub.drivingpromise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yikihyub.drivingpromise.R

/**
 * Pleos Connect 디자인 가이드의 색 토큰 (document.pleos.ai › Foundations › Colors), 낮/밤 두 벌.
 * 시스템 UI 옆에서 같은 차의 일부처럼 보이게 하는 것이 목적이다.
 *
 * 상태색의 역할은 고정한다 (색만으로 구분하지 않고 늘 글자와 함께 쓴다):
 *  - kept    informative_active  초록  지킨 약속, 정상
 *  - info    informative_positive 파랑  안내 중, 선택됨
 *  - caution regulation_orange   주황  지키기 어려운 약속, 주의
 *  - alarm   informative_negative 빨강  페달 확인 안내에만
 *  - sun     regulation_yellow   노랑  낮 시간, 해
 */
@Immutable
data class PleosColors(
    val dark: Boolean,
    val bg: Color,            // background_primary
    val surface: Color,       // surface_basic
    val surfaceHigh: Color,   // surface_high
    val surfaceAccent: Color, // surface_accent
    val ink: Color,           // text primary
    val ink2: Color,          // text secondary (84%)
    val ink3: Color,          // text tertiary (64%)
    val ink4: Color,          // text quaternary (32%)
    val line: Color,
    val filled: Color,        // button_filled_enabled
    val onFilled: Color,
    val kept: Color,
    val info: Color,
    val caution: Color,
    val alarm: Color,
    val sun: Color,
    val night: Color,
)

private val Light = PleosColors(
    dark = false,
    bg = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFEDEEF2), surfaceAccent = Color(0xFFE0E1E6),
    ink = Color(0xFF131417), ink2 = Color(0xD6131417), ink3 = Color(0xA3131417), ink4 = Color(0x52131417),
    line = Color(0xFFE0E1E6),
    filled = Color(0xFF44464E), onFilled = Color(0xFFFFFFFF),
    kept = Color(0xFF02C265), info = Color(0xFF0064FF), caution = Color(0xFFFF8A00),
    alarm = Color(0xFFFE3D16), sun = Color(0xFFFFC224), night = Color(0xFF44464E),
)

private val Dark = PleosColors(
    dark = true,
    bg = Color(0xFF1D1E21),
    surface = Color(0xFF131417), surfaceHigh = Color(0xFF313236), surfaceAccent = Color(0xFF44464E),
    ink = Color(0xFFFFFFFF), ink2 = Color(0xD6FFFFFF), ink3 = Color(0xA3FFFFFF), ink4 = Color(0x52FFFFFF),
    line = Color(0xFF313236),
    filled = Color(0xFFE0E1E6), onFilled = Color(0xFF131417),
    kept = Color(0xFF00E074), info = Color(0xFF0082FF), caution = Color(0xFFFF8A00),
    alarm = Color(0xFFFF4C28), sun = Color(0xFFFFC224), night = Color(0xFF6A57FF),
)

/** 테마와 무관하게 늘 어두운 면 (Pleos basic_static_dark / static_light). 페달 경고에 쓴다. */
object PleosStatic {
    val dark = Color(0xFF131417)
    val light = Color(0xFFFFFFFF)
    val light2 = Color(0xD6FFFFFF)
    val light3 = Color(0xA3FFFFFF)
    val light4 = Color(0x52FFFFFF)
}

val LocalPleos = staticCompositionLocalOf { Light }

/** 화면 코드에서 `Pleos.ink` 처럼 쓴다. 현재 낮/밤 테마의 값을 돌려준다. */
object Pleos {
    val c: PleosColors @Composable @ReadOnlyComposable get() = LocalPleos.current
    val bg: Color @Composable @ReadOnlyComposable get() = c.bg
    val surface: Color @Composable @ReadOnlyComposable get() = c.surface
    val surfaceHigh: Color @Composable @ReadOnlyComposable get() = c.surfaceHigh
    val surfaceAccent: Color @Composable @ReadOnlyComposable get() = c.surfaceAccent
    val ink: Color @Composable @ReadOnlyComposable get() = c.ink
    val ink2: Color @Composable @ReadOnlyComposable get() = c.ink2
    val ink3: Color @Composable @ReadOnlyComposable get() = c.ink3
    val ink4: Color @Composable @ReadOnlyComposable get() = c.ink4
    val line: Color @Composable @ReadOnlyComposable get() = c.line
    val filled: Color @Composable @ReadOnlyComposable get() = c.filled
    val onFilled: Color @Composable @ReadOnlyComposable get() = c.onFilled
    val kept: Color @Composable @ReadOnlyComposable get() = c.kept
    val info: Color @Composable @ReadOnlyComposable get() = c.info
    val caution: Color @Composable @ReadOnlyComposable get() = c.caution
    val alarm: Color @Composable @ReadOnlyComposable get() = c.alarm
    val sun: Color @Composable @ReadOnlyComposable get() = c.sun
    val night: Color @Composable @ReadOnlyComposable get() = c.night
}

val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal),
    Font(R.font.asta_sans, FontWeight.Medium),
    Font(R.font.asta_sans, FontWeight.SemiBold),
    Font(R.font.asta_sans, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Float = 1.4f) =
    TextStyle(fontFamily = AstaSans, fontSize = size.sp, fontWeight = weight, lineHeight = (size * lineHeight).sp)

/**
 * Pleos 가이드: 최소 18px, 행간 1.5배 권장, 경고 같은 핵심 정보 4mm 이상, 한 문장 3줄 이내.
 * 에뮬레이터 160dpi 기준 1sp ≈ 1px. 고령 운전자라 가장 작은 글자도 22sp.
 */
private val Type = Typography(
    displayLarge = style(104, FontWeight.Bold, 1.1f),
    displayMedium = style(64, FontWeight.Bold, 1.15f),
    headlineLarge = style(44, FontWeight.Bold, 1.3f),
    headlineMedium = style(36, FontWeight.SemiBold, 1.35f),
    titleLarge = style(30, FontWeight.SemiBold),
    titleMedium = style(27, FontWeight.Medium),
    bodyLarge = style(27, FontWeight.Normal, 1.5f),
    bodyMedium = style(24, FontWeight.Normal, 1.5f),
    bodySmall = style(22, FontWeight.Normal, 1.5f),
    labelLarge = style(26, FontWeight.SemiBold),
    labelMedium = style(23, FontWeight.Medium),
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** 차의 낮/밤 모드(uiMode)를 그대로 따른다. Pleos 가이드의 Day/Night 지원 요구. */
@Composable
fun PromiseTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val roles = if (dark) darkColorScheme(
        primary = c.filled, onPrimary = c.onFilled, secondary = c.info, background = c.bg, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.ink3,
        outline = c.line, outlineVariant = c.line, error = c.alarm,
    ) else lightColorScheme(
        primary = c.filled, onPrimary = c.onFilled, secondary = c.info, background = c.bg, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.ink3,
        outline = c.line, outlineVariant = c.line, error = c.alarm,
    )
    CompositionLocalProvider(LocalPleos provides c) {
        MaterialTheme(colorScheme = roles, typography = Type, shapes = Shape, content = content)
    }
}
