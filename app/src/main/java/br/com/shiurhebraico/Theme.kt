package br.com.shiurhebraico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The site's tokens (W05 src/global.css): tekhelet on cool paper, and the dark set. */
@Immutable
data class Palette(
    val paper: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val tekhelet: Color,
    val tekheletInk: Color,
    val wash: Color,
    val onTekhelet: Color,
    val done: Color,
    val novo: Color,
)

val LightPalette = Palette(
    paper = Color(0xFFF4F6F9), surface = Color(0xFFFFFFFF), ink = Color(0xFF16213A), muted = Color(0xFF586379),
    line = Color(0xFFDBE1EA), tekhelet = Color(0xFF23509A), tekheletInk = Color(0xFF173A73), wash = Color(0xFFE6EDF8),
    onTekhelet = Color.White, done = Color(0xFF2F7A55), novo = Color(0xFFB4462B),
)
val DarkPalette = Palette(
    paper = Color(0xFF0E1420), surface = Color(0xFF151D2C), ink = Color(0xFFE6EBF3), muted = Color(0xFF9AA6BA),
    line = Color(0xFF26314A), tekhelet = Color(0xFF86AAEA), tekheletInk = Color(0xFFB3CBF3), wash = Color(0xFF1B2840),
    onTekhelet = Color(0xFF0E1420), done = Color(0xFF6FC29A), novo = Color(0xFFF08C6E),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

private fun variable(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Sans = FontFamily(variable(R.font.inter, 400), variable(R.font.inter, 500), variable(R.font.inter, 600), variable(R.font.inter, 700))
val Serif = FontFamily(variable(R.font.frank_ruhl_libre, 400), variable(R.font.frank_ruhl_libre, 500), variable(R.font.frank_ruhl_libre, 700))

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = Serif, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
)

@Composable
fun ShiurTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(primary = p.tekhelet, onPrimary = p.onTekhelet, background = p.paper, onBackground = p.ink, surface = p.surface, onSurface = p.ink, surfaceVariant = p.wash, onSurfaceVariant = p.muted, outline = p.line)
    } else {
        lightColorScheme(primary = p.tekhelet, onPrimary = p.onTekhelet, background = p.paper, onBackground = p.ink, surface = p.surface, onSurface = p.ink, surfaceVariant = p.wash, onSurfaceVariant = p.muted, outline = p.line)
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
