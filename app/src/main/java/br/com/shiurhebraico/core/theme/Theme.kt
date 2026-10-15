package br.com.shiurhebraico.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.shiurhebraico.R

/** The site's Ink & Pearl tokens (styles/global.css) and the video library's cinema palette (styles/videos.css). */
object Ink {
    val ink = Color(0xFF121214)
    val pearl = Color(0xFFECE9E2)
    val paper = Color(0xFFF7F5F1)

    // the cinema (video library)
    val cinBg = Color(0xFF111111)
    val cinElev = Color(0xFF1A1A1A)
    val cinSurface = Color(0xFF222222)
    val cinText = Color(0xFFE5E5E5)
    val cinDim = Color(0xFFAAAAAA)
    val cinMuted = Color(0xFF666666)
    val cinBorder = Color(0x24FFFFFF)

    /** the AI mark (✦ IA): the site's --gold-mid on the dark library */
    val gold = Color(0xFFD9D9D9)
    /** captions only (CC): silver */
    val silver = Color(0xC7FFFFFF)
}

private fun variable(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val InterFamily = FontFamily(variable(R.font.inter, 400), variable(R.font.inter, 500), variable(R.font.inter, 600), variable(R.font.inter, 700))
// Shiur Hebraico (2026-10-15): the study speaks the app's own type — Libre-Franklin-like sans for labels, Frank Ruhl for titles
val MonoFamily = InterFamily
val DisplayFamily = FontFamily(variable(R.font.frank_ruhl_libre, 400), variable(R.font.frank_ruhl_libre, 500), variable(R.font.frank_ruhl_libre, 700))
val HebrewFamily = FontFamily(variable(R.font.frank_ruhl_libre, 400), variable(R.font.frank_ruhl_libre, 500), variable(R.font.frank_ruhl_libre, 700))

/** the body Hebrew, as the site's --font-hebrew: Noto Serif Hebrew carries the te'amim (cantillation) */
val HebrewTextFamily = FontFamily(variable(R.font.noto_serif_hebrew, 400), variable(R.font.noto_serif_hebrew, 600))

/** the site's small uppercase mono labels */
val MonoLabel = TextStyle(fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = InterFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = MonoLabel,
    labelMedium = MonoLabel.copy(fontSize = 11.sp),
)

// Shiur Hebraico's tekhelet on cool paper (and its dark set) instead of Emunah Shlemah's ink & pearl
private val pearlScheme = lightColorScheme(
    primary = Color(0xFF23509A), onPrimary = Color.White,
    background = Color(0xFFF4F6F9), onBackground = Color(0xFF16213A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF16213A),
    surfaceVariant = Color(0xFFE6EDF8), onSurfaceVariant = Color(0xFF586379),
    secondaryContainer = Color(0xFFE6EDF8), onSecondaryContainer = Color(0xFF173A73),
    outline = Color(0xFFDBE1EA), outlineVariant = Color(0xFFDBE1EA),
)

private val inkScheme = darkColorScheme(
    primary = Color(0xFF86AAEA), onPrimary = Color(0xFF0E1420),
    background = Color(0xFF0E1420), onBackground = Color(0xFFE6EBF3),
    surface = Color(0xFF151D2C), onSurface = Color(0xFFE6EBF3),
    surfaceVariant = Color(0xFF1B2840), onSurfaceVariant = Color(0xFF9AA6BA),
    secondaryContainer = Color(0xFF1B2840), onSecondaryContainer = Color(0xFFB3CBF3),
    outline = Color(0xFF26314A), outlineVariant = Color(0xFF26314A),
)

@Composable
fun EmunahTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) inkScheme else pearlScheme, typography = typography, content = content)
}

/** The video library is always the dark cinema, as on the site. */
@Composable
fun CinemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = inkScheme, typography = typography, content = content)
}
