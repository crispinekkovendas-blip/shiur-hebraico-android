package br.com.shiurhebraico.siddur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.shiurhebraico.core.hebrew.applyMarks
import br.com.shiurhebraico.core.hebrew.skeletonTranslit
import br.com.shiurhebraico.core.hebrew.stripNikud
import br.com.shiurhebraico.core.net.SiteClient
import br.com.shiurhebraico.core.theme.DisplayFamily
import br.com.shiurhebraico.core.theme.HebrewFamily
import br.com.shiurhebraico.core.theme.HebrewTextFamily
import br.com.shiurhebraico.core.theme.MonoLabel
import br.com.shiurhebraico.core.ui.Chip
import br.com.shiurhebraico.core.ui.GlossarySheet
import br.com.shiurhebraico.core.ui.MarksMenu
import br.com.shiurhebraico.core.ui.PhonSheet
import br.com.shiurhebraico.siddur.LEGEND
import br.com.shiurhebraico.siddur.ReadMode
import br.com.shiurhebraico.siddur.ReaderState
import br.com.shiurhebraico.siddur.SiddurHomeViewModel
import br.com.shiurhebraico.siddur.SiddurReaderViewModel
import br.com.shiurhebraico.siddur.SiddurSection
import br.com.shiurhebraico.siddur.SiddurSpan
import br.com.shiurhebraico.siddur.SiddurVerse
import br.com.shiurhebraico.siddur.SiddurWord
import br.com.shiurhebraico.siddur.coversVerse
import br.com.shiurhebraico.siddur.groupWords
import br.com.shiurhebraico.siddur.insertPt
import br.com.shiurhebraico.siddur.legendOf
import br.com.shiurhebraico.siddur.pillText
import br.com.shiurhebraico.siddur.spanAt
import br.com.shiurhebraico.siddur.spanStarts
import br.com.shiurhebraico.videos.sidur.labelPt

/** the rubric colours: chazan ochre, kahal ink, occasion teal (the site's sid-rubric) */
private fun roleColor(kind: String?): Color = when (kind) {
    "chazan" -> Color(0xFFA0661A)
    "kahal" -> Color(0xFF3B5B85)
    "occasion" -> Color(0xFF2E7D7A)
    else -> Color.Unspecified
}

/** the role a word takes: none when the span only says the verse is left out */
private fun roleOf(span: SiddurSpan?) = span?.takeIf { !it.omit }?.kind

@Composable
private fun Rubric(span: SiddurSpan) {
    val col = roleColor(span.kind)
    val heb = span.kind != "occasion"
    Text(
        pillText(span),
        style = if (heb) TextStyle(fontFamily = HebrewFamily, fontSize = 13.sp, textDirection = TextDirection.Rtl) else MonoLabel.copy(fontSize = 9.5.sp, letterSpacing = 0.4.sp),
        color = Color.White,
        modifier = Modifier.padding(horizontal = 2.dp).clip(RoundedCornerShape(999.dp)).background(col).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/* ── the home: Agora, Continuar, search, the six categories ──────────── */

@Composable
fun SiddurHomeScreen(vm: SiddurHomeViewModel, onOpen: (String, Int) -> Unit) {
    val s by vm.state.collectAsState()
    val c = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    var glossary by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshLast() }
    LazyColumn(Modifier.fillMaxSize().background(c.background), contentPadding = PaddingValues(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("סִדּוּר", style = TextStyle(fontFamily = HebrewFamily, fontSize = 40.sp), color = c.onBackground)
                Text("Siddur", fontFamily = DisplayFamily, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
                Text("EDOT HAMIZRACH", style = MonoLabel, color = c.onSurfaceVariant)
                if (s.sections.isNotEmpty()) {
                    Text(
                        "${s.sections.size} orações · ${s.cats.size} categorias · ${s.cats.sumOf { it.services.size }} serviços",
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (s.nowSlug.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.primary).clickable { onOpen(s.nowSlug, 0) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AGORA", style = MonoLabel, color = c.onPrimary.copy(alpha = 0.7f))
                    Spacer(Modifier.width(10.dp))
                    Text(s.nowLabel, style = MaterialTheme.typography.titleMedium, color = c.onPrimary, modifier = Modifier.weight(1f))
                    Text("→", color = c.onPrimary)
                }
            }
            s.last?.let { l ->
                Text(
                    "Continuar · ${l.prayer}" + if (l.service.isNotEmpty()) " · ${labelPt(l.service)}" else "",
                    style = MaterialTheme.typography.bodyMedium, color = c.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.outline, RoundedCornerShape(8.dp))
                        .clickable { onOpen(l.slug, 0) }.padding(12.dp),
                )
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("GLOSSÁRIO", false) { glossary = true }
                Chip("SIDUR EM PDF", false) { uri.openUri("${SiteClient.BASE}/siddur/livro/") }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.outline, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (s.search.isEmpty()) Text("Buscar oração, serviço, categoria…", color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    BasicTextField(s.search, vm::setSearch, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onBackground), cursorBrush = SolidColor(c.onBackground), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        when {
            s.loading && s.sections.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
            s.error -> item { Retry("Não foi possível carregar o Sidur.") { vm.load() } }
            s.search.isNotBlank() -> {
                val res = vm.results(s)
                item { Text("${res.size} resultados", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) }
                items(res, key = { it.slug }) { x -> PrayerRow(x, sub = "${labelPt(x.category)} · ${labelPt(x.service)}") { onOpen(x.slug, 0) } }
            }
            else -> items(s.cats, key = { it.key }) { g ->
                Column(Modifier.padding(top = 10.dp)) {
                    // the category button (the six buttons of the dashboard)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (s.cat == g.key) c.primary else c.surface)
                            .border(1.dp, c.outline, RoundedCornerShape(8.dp)).clickable { vm.openCat(g.key) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (s.cat == g.key) c.onPrimary.copy(alpha = 0.15f) else c.surfaceVariant), Alignment.Center) {
                            Text(g.letter, style = TextStyle(fontFamily = HebrewFamily, fontSize = 18.sp), color = if (s.cat == g.key) c.onPrimary else c.onSurface)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(labelPt(g.label), style = MaterialTheme.typography.titleMedium, color = if (s.cat == g.key) c.onPrimary else c.onSurface, modifier = Modifier.weight(1f))
                        Text("${g.count}", style = MonoLabel, color = if (s.cat == g.key) c.onPrimary.copy(alpha = 0.7f) else c.onSurfaceVariant)
                        Text(if (s.cat == g.key) "  ▾" else "  ▸", color = if (s.cat == g.key) c.onPrimary else c.onSurfaceVariant)
                    }
                    if (s.cat == g.key) {
                        g.services.forEach { svc ->
                            val k = "${g.key}/${svc.label}"
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp).clip(RoundedCornerShape(6.dp)).background(if (s.svc == k) c.surfaceVariant else Color.Transparent)
                                    .border(1.dp, c.outline, RoundedCornerShape(6.dp)).clickable { vm.openSvc(k) }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(labelPt(svc.label), style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                                Text("${svc.items.size}", style = MonoLabel, color = c.onSurfaceVariant)
                                Text(if (s.svc == k) "  ▾" else "  ▸", color = c.onSurfaceVariant)
                            }
                            if (s.svc == k) svc.items.forEach { x -> Box(Modifier.padding(start = 28.dp)) { PrayerRow(x, sub = x.incipitTr) { onOpen(x.slug, 0) } } }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (glossary) GlossarySheet { glossary = false }
}

@Composable
private fun PrayerRow(x: SiddurSection, sub: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(x.prayer.ifEmpty { x.heading }, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, modifier = Modifier.weight(1f))
            if (x.partCount > 1) Text("${x.partCount} partes", style = MonoLabel, color = c.onSurfaceVariant)
        }
        if (x.incipitHe.isNotEmpty()) Text(x.incipitHe, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 15.sp, textDirection = TextDirection.Rtl), color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Retry(text: String, retry: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Chip("TENTAR DE NOVO", true, onClick = retry)
    }
}

/* ── the reader ───────────────────────────────────────────────────────── */

private data class Phon(val he: String, val tr: String, val pt: String)

@Composable
fun SiddurReaderScreen(vm: SiddurReaderViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    val c = MaterialTheme.colorScheme
    var phon by remember { mutableStateOf<Phon?>(null) }
    var glossary by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(s.slug, s.part) { list.scrollToItem(0) }

    val page = s.page
    val sec = page?.section
    val prev = s.siblings.firstOrNull { it.slug == sec?.prev }
    val next = s.siblings.firstOrNull { it.slug == sec?.next }

    Column(Modifier.fillMaxSize().background(c.background)) {
        // the masthead
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = c.onBackground, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sec?.prayer ?: "", style = MaterialTheme.typography.titleMedium, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val heading = sec?.parts?.getOrNull(s.part)?.heading.orEmpty()
                Text(
                    listOfNotNull(sec?.let { labelPt(it.service) }, heading.ifEmpty { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (!sec?.incipitHe.isNullOrEmpty()) Text(sec!!.incipitHe, style = TextStyle(fontFamily = HebrewFamily, fontSize = 16.sp), color = c.onSurfaceVariant, maxLines = 1)
        }
        if ((sec?.parts?.size ?: 0) > 1) {
            LazyRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sec!!.parts) { p -> Chip("${p.index + 1}", p.index == s.part) { vm.setPart(p.index) } }
            }
        }
        // the controls: modes · Prática · Glossário · Sinais · H/T/P · direction
        LazyRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            items(ReadMode.entries) { m -> Chip(m.label.uppercase(), s.mode == m) { vm.setMode(m) } }
            if (s.mode != ReadMode.PORTUGUES) item { Chip("PRÁTICA", s.practice) { vm.togglePractice() } }
            item { Chip("GLOSSÁRIO", false) { glossary = true } }
            item { MarksMenu(s.marks, vm::setMarks) }
            if (s.mode == ReadMode.ESTUDO) {
                item { Chip("H", s.layers.he) { vm.toggleLayer("he") } }
                item { Chip("T", s.layers.tr) { vm.toggleLayer("tr") } }
                item { Chip("P", s.layers.pt) { vm.toggleLayer("pt") } }
                item { Chip(if (s.rtl) "◂ RTL" else "LTR ▸", true) { vm.setRtl(!s.rtl) } }
            }
        }

        when {
            s.loading && page == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Carregando…", color = c.onSurfaceVariant) }
            s.error && page == null -> Retry("Não foi possível carregar esta parte.") { vm.load() }
            page != null -> LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                val legend = legendOf(page.verses)
                if (legend.isNotEmpty()) item {
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        legend.forEach { k ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(roleColor(k)))
                                Spacer(Modifier.width(4.dp))
                                Text(LEGEND[k] ?: k, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = c.onSurfaceVariant)
                            }
                        }
                    }
                }
                items(page.verses, key = { "${s.slug}#${s.part}/${it.index}" }) { v ->
                    VerseView(v, s, onWord = { w -> phon = Phon(w.he, w.tr, w.pt) }, onNote = { vm.toggleNote(v.index) })
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                        Column(Modifier.weight(1f).clickable(enabled = prev != null) { prev?.let { vm.go(it.slug) } }) {
                            Text("ANTERIOR", style = MonoLabel, color = c.onSurfaceVariant)
                            Text(prev?.let { "← ${it.prayer}" } ?: "", style = MaterialTheme.typography.bodySmall, color = c.onBackground)
                        }
                        Column(Modifier.weight(1f).clickable(enabled = next != null) { next?.let { vm.go(it.slug) } }, horizontalAlignment = Alignment.End) {
                            Text("PRÓXIMA ORAÇÃO", style = MonoLabel, color = c.onSurfaceVariant)
                            Text(next?.let { "${it.prayer} →" } ?: "", style = MaterialTheme.typography.bodySmall, color = c.onBackground, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
    phon?.let { p -> PhonSheet(p.he, p.tr, p.pt) { phon = null } }
    if (glossary) GlossarySheet { glossary = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerseView(v: SiddurVerse, s: ReaderState, onWord: (SiddurWord) -> Unit, onNote: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val he = { t: String -> if (s.practice) stripNikud(t) else applyMarks(t, s.marks) }
    val tr = { w: SiddurWord -> if (s.practice) skeletonTranslit(w.he, w.tr).ifEmpty { "·" } else w.tr.ifEmpty { "·" } }
    val heStyle = TextStyle(fontFamily = HebrewTextFamily, fontSize = 22.sp, lineHeight = 34.sp, textDirection = TextDirection.Rtl)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (v.isHeader) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Text(he(v.words.joinToString(" ") { it.he }), style = heStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), color = c.onBackground)
                    v.spans.forEach { Rubric(it) }
                }
            }
            if (v.labelPt.isNotEmpty()) Text(v.labelPt, style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
            return@Column
        }
        when (s.mode) {
            ReadMode.HEBRAICO -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    v.words.forEach { w ->
                        spanStarts(v.spans, w.idx).forEach { Rubric(it) }
                        val role = roleOf(spanAt(v.spans, w.idx))
                        Text(he(w.he), style = heStyle, color = if (role != null) roleColor(role) else c.onBackground, modifier = Modifier.clickable { onWord(w) })
                    }
                }
            }
            ReadMode.PORTUGUES -> {
                val whole = v.spans.firstOrNull { coversVerse(it, v.words) }
                v.spans.filter { coversVerse(it, v.words) }.forEach { Row { Rubric(it) } }
                Text(v.translationPt.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge, color = roleOf(whole)?.let(::roleColor) ?: c.onBackground)
                v.spans.filter { !coversVerse(it, v.words) }.forEach { sp ->
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Rubric(sp)
                        Text(" " + insertPt(v.words, sp).ifEmpty { "·" }, style = MaterialTheme.typography.bodyMedium, color = roleOf(sp)?.let(::roleColor) ?: c.onBackground)
                    }
                }
            }
            ReadMode.ESTUDO -> CompositionLocalProvider(LocalLayoutDirection provides if (s.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupWords(v.words).forEach { g ->
                        val first = g.words.first().idx
                        val role = roleOf(spanAt(v.spans, first))
                        val col = role?.let(::roleColor)
                        Column(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(col?.copy(alpha = 0.10f) ?: c.surface)
                                .border(1.dp, col?.copy(alpha = 0.5f) ?: c.outline, RoundedCornerShape(6.dp)).padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val starts = spanStarts(v.spans, first)
                            if (starts.isNotEmpty()) Row { starts.forEach { Rubric(it) } }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                g.words.forEach { w ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onWord(w) }) {
                                        if (s.layers.he) Text(he(w.he), style = heStyle.copy(fontSize = 21.sp, lineHeight = 30.sp), color = c.onBackground)
                                        if (s.layers.tr) Text(tr(w), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, textDirection = TextDirection.Ltr), color = c.onSurfaceVariant)
                                    }
                                }
                            }
                            if (s.layers.pt) Text(g.pt.ifEmpty { "·" }, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, textDirection = TextDirection.Ltr), color = if (g.pt.isEmpty()) c.onSurfaceVariant.copy(alpha = 0.4f) else c.onSurface, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        if (v.notesPt.isNotEmpty()) {
            Chip(if (v.notesPt.length < 80) "NOTA" else "COMENTÁRIO", v.index in s.openNotes, onClick = onNote)
            if (v.index in s.openNotes) Text(v.notesPt, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
