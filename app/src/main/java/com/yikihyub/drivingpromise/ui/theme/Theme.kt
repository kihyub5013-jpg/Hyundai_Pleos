package com.yikihyub.drivingpromise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yikihyub.drivingpromise.R

/**
 * Pleos Connect 기본 앱(차량 설정·내비)과 같은 톤.
 * 흰 바탕, 얇은 회색 선, 선택 항목은 검정, 상태값은 파랑. 폰트는 시스템 UI와 같은 Asta Sans.
 * 에뮬레이터 화면이 2560×1440 @160dpi 라 1dp ≈ 1px. 숫자는 그 기준.
 */
object Pleos {
    val bg = Color(0xFFFFFFFF)
    val surface = Color(0xFFF2F2F2)     // 선택된 타일, 블록 배경
    val line = Color(0xFFE4E4E4)        // 타일·리스트 구분선
    val ink = Color(0xFF111111)         // 본문
    val ink2 = Color(0xFF767676)        // 보조 텍스트
    val ink3 = Color(0xFFB0B0B0)        // 비활성
    val black = Color(0xFF000000)       // 선택·주요 버튼
    val accent = Color(0xFF2A6BF0)      // 상태값 (차량 설정의 "잠김")
    val ok = Color(0xFF00A862)
    val warn = Color(0xFFE8641B)
    val danger = Color(0xFFDA3D2A)
}

val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal),
    Font(R.font.asta_sans, FontWeight.Medium),
    Font(R.font.asta_sans, FontWeight.SemiBold),
    Font(R.font.asta_sans, FontWeight.Bold),
)

private val Scheme = lightColorScheme(
    primary = Pleos.black,
    onPrimary = Color.White,
    secondary = Pleos.accent,
    onSecondary = Color.White,
    tertiary = Pleos.warn,
    background = Pleos.bg,
    onBackground = Pleos.ink,
    surface = Pleos.bg,
    onSurface = Pleos.ink,
    surfaceVariant = Pleos.surface,
    onSurfaceVariant = Pleos.ink2,
    outline = Pleos.line,
    outlineVariant = Pleos.line,
    error = Pleos.danger,
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int = (size * 1.4).toInt()) =
    TextStyle(fontFamily = AstaSans, fontSize = size.sp, fontWeight = weight, lineHeight = lineHeight.sp)

private val Type = Typography(
    displayLarge = style(112, FontWeight.Bold, 112),
    displayMedium = style(72, FontWeight.Bold, 76),
    headlineLarge = style(44, FontWeight.SemiBold),
    headlineMedium = style(36, FontWeight.SemiBold),
    titleLarge = style(30, FontWeight.SemiBold),
    titleMedium = style(26, FontWeight.Medium),
    bodyLarge = style(26, FontWeight.Normal),
    bodyMedium = style(23, FontWeight.Normal),
    bodySmall = style(20, FontWeight.Normal),
    labelLarge = style(25, FontWeight.Medium),
    labelMedium = style(22, FontWeight.Medium),
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PromiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = Shape, content = content)
}
