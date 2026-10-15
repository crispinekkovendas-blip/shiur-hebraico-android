package br.com.shiurhebraico

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun Portrait(rav: Rav?, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier.size(size).clip(CircleShape).background(p.wash).border(1.dp, p.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(rav?.name ?: "?"), color = p.tekhelet, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
        if (!rav?.photo.isNullOrBlank()) {
            AsyncImage(model = BASE + rav!!.photo, contentDescription = rav.name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
        }
    }
}

@Composable
fun Tag(text: String, strong: Boolean = false, color: Color? = null) {
    val p = LocalPalette.current
    val c = color ?: p.tekhelet
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = if (strong) Color.White else c,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (strong) c else p.wash)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** "Novo! 5787" for this year's lessons, the Hebrew year for older ones */
@Composable
fun YearTag(y: Int) {
    if (y <= 0) return
    val p = LocalPalette.current
    if (y == thisHebYear()) Tag("Novo! $y", strong = true, color = p.novo) else Tag("$y")
}

@Composable
fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = p.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = p.tekhelet, modifier = Modifier.clickable(onClick = onAction).padding(6.dp))
        }
    }
}

@Composable
fun Thumb(l: Lesson, modifier: Modifier = Modifier, progress: Float = 0f) {
    val p = LocalPalette.current
    Box(modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(p.wash)) {
        AsyncImage(model = l.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Text(
            duration(l.d),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000)).padding(horizontal = 5.dp, vertical = 1.dp),
        )
        Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { YearTag(l.y) }
        if (progress > 0.01f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                color = p.tekhelet,
                trackColor = Color(0x55000000),
            )
        }
    }
}

/** a card for sideways rails: still, place, title, teacher */
@Composable
fun LessonCard(c: Catalog, l: Lesson, width: Dp, progress: Float = 0f, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val rav = c.ravBySlug[l.r]
    Column(Modifier.width(width).clip(RoundedCornerShape(10.dp)).clickable { onOpen(l.id) }) {
        Thumb(l, Modifier.fillMaxWidth(), progress)
        Spacer(Modifier.height(8.dp))
        Text(c.placeOf(l).uppercase(), style = MaterialTheme.typography.labelSmall, color = p.tekhelet, maxLines = 1)
        Text(l.title, style = MaterialTheme.typography.titleSmall, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Text(rav?.name ?: "", style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/** a compact list row: still on the left, text on the right */
@Composable
fun LessonRow(c: Catalog, l: Lesson, number: Int? = null, showRav: Boolean = true, progress: Float = 0f, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val rav = c.ravBySlug[l.r]
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(l.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (number != null) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.width(22.dp).padding(top = 4.dp), textAlign = TextAlign.End)
        }
        Thumb(l, Modifier.width(128.dp), progress)
        Column(Modifier.weight(1f)) {
            Text(c.placeOf(l).uppercase(), style = MaterialTheme.typography.labelSmall, color = p.tekhelet, maxLines = 1)
            Text(l.title, style = MaterialTheme.typography.titleSmall, color = p.ink, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            if (showRav && rav != null) Text(rav.name, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun HebrewText(text: String, size: Int, color: Color, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Medium, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        fontFamily = Serif,
        fontSize = size.sp,
        fontWeight = weight,
        color = color,
        modifier = modifier,
        textAlign = align,
        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, lineHeight = (size * 1.35f).sp),
    )
}

/**
 * Year tabs (2026-10-15): one pill per Hebrew year the lessons have, newest first ("Novo! 5787" for
 * this year), optionally "Todos" first. The selected pill slides its colour in; the row scrolls the
 * selection into view. Our own pills, not a system dropdown.
 */
@Composable
fun YearTabs(
    years: List<Int>,
    selected: Int,
    counts: Map<Int, Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    all: Boolean = false,
) {
    val p = LocalPalette.current
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    val tabs = (if (all) listOf(0) else emptyList()) + years
    androidx.compose.runtime.LaunchedEffect(selected) {
        val i = tabs.indexOf(selected)
        if (i >= 0) state.animateScrollToItem((i - 1).coerceAtLeast(0))
    }
    androidx.compose.foundation.lazy.LazyRow(
        modifier.fillMaxWidth().background(p.paper),
        state = state,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs.size, key = { tabs[it] }) { i ->
            val y = tabs[i]
            val on = y == selected
            val novo = y == thisHebYear()
            val accent = if (novo) p.novo else p.tekhelet
            val bg by androidx.compose.animation.animateColorAsState(if (on) accent else p.surface, androidx.compose.animation.core.tween(220), label = "bg")
            val fg by androidx.compose.animation.animateColorAsState(if (on) Color.White else (if (novo) p.novo else p.ink), androidx.compose.animation.core.tween(220), label = "fg")
            val border by androidx.compose.animation.animateColorAsState(if (on) accent else p.line, androidx.compose.animation.core.tween(220), label = "bd")
            val scale by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0.96f, androidx.compose.animation.core.spring(dampingRatio = 0.6f), label = "sc")
            Row(
                Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(50))
                    .clickable { onSelect(y) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when { y == 0 -> "Todos"; novo -> "Novo! $y"; else -> "$y" },
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
                val n = if (y == 0) counts.values.sum() else counts[y] ?: 0
                Text(" $n", style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
            }
        }
    }
}
