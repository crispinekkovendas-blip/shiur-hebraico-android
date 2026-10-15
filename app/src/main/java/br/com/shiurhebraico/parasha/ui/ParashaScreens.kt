package br.com.shiurhebraico.parasha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.shiurhebraico.core.DEFAULT_PLACE
import br.com.shiurhebraico.core.Zmanim
import br.com.shiurhebraico.core.hebrew.Marks
import br.com.shiurhebraico.core.hebrew.applyMarks
import br.com.shiurhebraico.core.theme.DisplayFamily
import br.com.shiurhebraico.core.theme.HebrewFamily
import br.com.shiurhebraico.core.theme.HebrewTextFamily
import br.com.shiurhebraico.core.theme.MonoLabel
import br.com.shiurhebraico.core.ui.Chip
import br.com.shiurhebraico.core.ui.MarksMenu
import br.com.shiurhebraico.core.ui.TaggedText
import br.com.shiurhebraico.parasha.Comment
import br.com.shiurhebraico.parasha.Commentary
import br.com.shiurhebraico.parasha.PSection
import br.com.shiurhebraico.parasha.Parasha
import br.com.shiurhebraico.parasha.ParashaListViewModel
import br.com.shiurhebraico.parasha.ParashaState
import br.com.shiurhebraico.parasha.ParashaViewModel
import br.com.shiurhebraico.parasha.Verse
import br.com.shiurhebraico.parasha.chapterVerse
import br.com.shiurhebraico.siddur.ui.Retry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ALIYAH_PT = listOf("Primeira", "Segunda", "Terceira", "Quarta", "Quinta", "Sexta", "Sétima")
private val ALIYAH_TR = listOf("Rishon", "Sheni", "Shlishi", "Revii", "Chamishi", "Shishi", "Shvii")
private val ALIYAH_HE = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שביעי")

/* ── the list: this week's parashá, then the five books ──────────────── */

@Composable
fun ParashaListScreen(vm: ParashaListViewModel, onOpen: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val c = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize().background(c.background), contentPadding = PaddingValues(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("פָּרָשַׁת הַשָּׁבוּעַ", style = TextStyle(fontFamily = HebrewFamily, fontSize = 28.sp), color = c.onBackground)
                Text("Parashá", fontFamily = DisplayFamily, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
                Text("AS 54 PARASHIOT DA TORÁ", style = MonoLabel, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            val cur = s.items.firstOrNull { it.key == s.current }
            if (cur != null) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.primary).clickable { onOpen(cur.key) }.padding(16.dp)) {
                    Text("ESTA SEMANA", style = MonoLabel, color = c.onPrimary.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cur.name, fontFamily = DisplayFamily, fontSize = 28.sp, color = c.onPrimary, modifier = Modifier.weight(1f))
                        Text(cur.hebrewName, style = TextStyle(fontFamily = HebrewFamily, fontSize = 24.sp), color = c.onPrimary)
                    }
                    Text(cur.traducao, style = MaterialTheme.typography.bodyMedium, color = c.onPrimary.copy(alpha = 0.85f))
                }
            }
        }
        when {
            s.loading && s.items.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
            s.error -> item { Retry("Não foi possível carregar as parashiot.") { vm.load() } }
            else -> s.books.forEach { b ->
                item(key = "b-${b.key}") {
                    Row(Modifier.padding(top = 20.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text(b.namePt, fontFamily = DisplayFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
                        Text("  ${b.trad}", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                    }
                }
                items(s.items.filter { it.book == b.key }, key = { it.key }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (p.key == s.current) c.surfaceVariant else Color.Transparent)
                            .clickable { onOpen(p.key) }.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${p.orderIndex}".padStart(2, '0'), style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.width(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                            Text(p.traducao, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        }
                        Text(p.hebrewName, style = TextStyle(fontFamily = HebrewFamily, fontSize = 18.sp), color = c.onSurface)
                    }
                }
            }
        }
    }
}

/* ── the dashboard ────────────────────────────────────────────────────── */

@Composable
fun ParashaScreen(vm: ParashaViewModel, onBack: () -> Unit, onOpenSiddur: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val c = MaterialTheme.colorScheme
    val p = s.parasha
    val list = rememberLazyListState()
    LaunchedEffect(s.section, s.aliyah, s.commentary) { list.scrollToItem(0) }
    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = c.onBackground, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p?.name ?: "", fontFamily = DisplayFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground, maxLines = 1)
                Text(listOf(p?.traducao, p?.bookName).filter { !it.isNullOrBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1)
            }
            Text(p?.hebrewName ?: "", style = TextStyle(fontFamily = HebrewFamily, fontSize = 22.sp), color = c.onBackground)
        }
        // a plain scrolling row: every tab is composed, so the highlight always follows the section
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PSection.entries.forEach { sec -> Chip(sec.label.uppercase(), s.section == sec) { vm.setSection(sec) } }
        }
        when {
            s.loading && p == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Carregando conteúdo da parashá…", color = c.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
            s.error -> Retry("Não foi possível carregar esta parashá.") { vm.load() }
            p != null -> LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                when (s.section) {
                    PSection.INICIO -> inicio(p, s, vm)
                    PSection.ESTUDO -> estudo(p, s, vm)
                    PSection.MITZVOT -> mitzvot(p, vm)
                    PSection.OUTRAS -> outras(p, s)
                    PSection.SHABAT -> shabat(s, onOpenSiddur)
                    PSection.HALACHA -> halacha(p, s, vm)
                    PSection.MIDRASH -> midrash(p, s)
                    PSection.ZOHAR -> zohar(s)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun Head(text: String, sub: String = "") {
    val c = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 16.dp, bottom = 6.dp)) {
        Text(text, fontFamily = DisplayFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

@Composable
private fun Empty(text: String, hint: String = "") {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        if (hint.isNotEmpty()) Text(hint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

/* ── Início ───────────────────────────────────────────────────────────── */

private fun LazyListScope.inicio(p: Parasha, s: ParashaState, vm: ParashaViewModel) {
    item {
        val c = MaterialTheme.colorScheme
        if (p.tagline.isNotEmpty()) Text(p.tagline, fontFamily = DisplayFamily, fontSize = 20.sp, color = c.onSurfaceVariant)
        val stats = listOf(
            "Versículos" to p.aliyot.sumOf { it.verses.size }.let { if (it > 0) "$it" else "—" },
            "Mandamentos" to "${p.mitzvot.size}",
            "Haftará" to p.haftaraRef.ifEmpty { "—" },
            "Tehilim" to (p.tehilim.takeIf { it.isNotEmpty() }?.let { "Salmo $it" } ?: "—"),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { (k, v) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).border(1.dp, c.outline, RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text(k.uppercase(), style = MonoLabel.copy(fontSize = 8.5.sp), color = c.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface, maxLines = 2)
                }
            }
        }
        if (p.temas.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { p.temas.forEach { Chip(it, false) {} } }
        }
        Head("Início · Sinopse")
        val synopsis = p.resumo.ifEmpty { s.content?.resumo ?: "" }
        if (synopsis.isNotEmpty()) TaggedText(synopsis, p.resolvers, MaterialTheme.typography.bodyLarge)
        Head("Resumo das aliyot")
    }
    val summaries = s.content?.aliyot.orEmpty()
    val rows = maxOf(p.aliyot.size, summaries.size, 7)
    items((0 until rows).toList()) { i ->
        val c = MaterialTheme.colorScheme
        val a = p.aliyot.getOrNull(i)
        val sm = summaries.getOrNull(i)
        Row(Modifier.fillMaxWidth().clickable { vm.jumpToAliyah(i) }.padding(vertical = 8.dp)) {
            Text("${i + 1}", fontFamily = DisplayFamily, fontSize = 22.sp, color = c.onSurfaceVariant, modifier = Modifier.width(30.dp))
            Column(Modifier.weight(1f)) {
                Text("${ALIYAH_TR.getOrElse(i) { "" }} · ${ALIYAH_HE.getOrElse(i) { "" }}", style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                val ref = sm?.verses?.ifEmpty { null } ?: a?.refPt
                if (!ref.isNullOrEmpty()) Text(ref, style = MonoLabel, color = c.onSurfaceVariant)
                if (!sm?.summary.isNullOrEmpty()) Text(sm!!.summary, style = MaterialTheme.typography.bodySmall, color = c.onSurface)
            }
        }
    }
    val coms = s.content?.comentarios.orEmpty()
    if (coms.isNotEmpty()) {
        item { Head("Comentários clássicos") }
        items(coms) { co ->
            val c = MaterialTheme.colorScheme
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(listOf(co.source, co.ref).filter { it.isNotEmpty() }.joinToString(" · ").uppercase(), style = MonoLabel, color = c.onSurfaceVariant)
                if (co.title.isNotEmpty()) Text(co.title, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                TaggedText(co.body, p.resolvers)
            }
        }
    }
}

/* ── Estudo: the Torah by aliyá, with a commentary ───────────────────── */

private fun heText(t: String, m: Marks) = applyMarks(t, m)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Interlinear(he: String, tr: String, ar: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val heWords = he.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val trWords = tr.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val heStyle = TextStyle(fontFamily = HebrewTextFamily, fontSize = if (ar) 18.sp else 22.sp, lineHeight = if (ar) 28.sp else 34.sp, textDirection = TextDirection.Rtl)
    if (heWords.isNotEmpty() && heWords.size == trWords.size) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                heWords.forEachIndexed { i, w ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(w, style = heStyle, color = if (ar) c.onSurfaceVariant else c.onBackground)
                        Text(trWords[i], style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, textDirection = TextDirection.Ltr), color = c.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        if (he.isNotEmpty()) Text(he, style = heStyle, color = if (ar) c.onSurfaceVariant else c.onBackground, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        if (tr.isNotEmpty()) Text(tr, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

/** the verse's Rashi: a JSON array of HTML strings, or one string */
private fun rashiHtml(raw: String): String = try {
    Json.parseToJsonElement(raw).jsonArray.joinToString("<br/><br/>") { it.jsonPrimitive.content }
} catch (_: Exception) {
    raw
}

private fun LazyListScope.estudo(p: Parasha, s: ParashaState, vm: ParashaViewModel) {
    val aliyah = p.aliyot.getOrNull(s.aliyah)
    val verses = aliyah?.verses.orEmpty()
    val anyAr = verses.any { it.ar.isNotEmpty() || it.arTr.isNotEmpty() }
    val anyRashi = verses.any { it.rashi.isNotEmpty() }
    val anySimple = verses.any { it.ptSimple.isNotEmpty() }
    item {
        val c = MaterialTheme.colorScheme
        Text(
            "${p.name} · ${ALIYAH_PT.getOrElse(s.aliyah) { "" }} Aliyá  ${ALIYAH_HE.getOrElse(s.aliyah) { "" }}",
            style = MaterialTheme.typography.titleSmall, color = c.onBackground,
        )
        if (!aliyah?.refPt.isNullOrEmpty()) Text(aliyah!!.refPt, style = MonoLabel, color = c.onSurfaceVariant)
        LazyRow(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items((0 until maxOf(p.aliyot.size, 1)).toList()) { i -> Chip("${i + 1}", i == s.aliyah) { vm.setAliyah(i) } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Commentary.entries) { k ->
                val enabled = k == Commentary.RASHI || s.commentaryOf(k).isNotEmpty()
                Chip(k.label.uppercase(), s.commentary == k, enabled) { vm.setCommentary(k) }
            }
        }
        LazyRow(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item { Chip("H", s.layers.he) { vm.toggleLayer("he") } }
            item { Chip("T", s.layers.tr) { vm.toggleLayer("tr") } }
            item { Chip("P", s.layers.pt) { vm.toggleLayer("pt") } }
            if (anyAr) item { Chip("A", s.layers.ar) { vm.toggleLayer("ar") } }
            if (anyRashi && s.commentary == Commentary.RASHI && s.commentaryOf(Commentary.RASHI).isEmpty()) item { Chip("R", s.layers.rashi) { vm.toggleLayer("rashi") } }
            item { MarksMenu(s.marks, vm::setMarks) }
            if (anySimple) {
                item { Chip("COMPLEXA", !s.simple) { vm.setSimple(false) } }
                item { Chip("SIMPLES", s.simple) { vm.setSimple(true) } }
            }
        }
    }
    val comm = s.commentaryOf(s.commentary)
    if (s.commentary != Commentary.RASHI || comm.isNotEmpty()) {
        commentaryPane(p, s, verses, comm)
        return
    }
    if (verses.isEmpty()) {
        item { Empty("Conteúdo da aliyá em preparação.") }
        return
    }
    itemsIndexed(verses, key = { i, v -> "${s.aliyah}/${v.ref}/$i" }) { i, v ->
        val c = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row {
                Text(v.ref, style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("VERSO ${(i + 1).toString().padStart(2, '0')}", style = MonoLabel, color = c.onSurfaceVariant)
            }
            if ((s.layers.he || s.layers.tr) && v.he.isNotEmpty()) Interlinear(if (s.layers.he) heText(v.he, s.marks) else "", if (s.layers.tr) v.tr else "")
            val pt = if (s.simple && v.ptSimple.isNotEmpty()) v.ptSimple else v.pt
            if (s.layers.pt && pt.isNotEmpty()) Text(pt, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, modifier = Modifier.padding(top = 6.dp))
            if (s.layers.ar && (v.ar.isNotEmpty() || v.arTr.isNotEmpty())) {
                Text("ONKELOS", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Interlinear(heText(v.ar, s.marks), if (s.layers.tr) v.arTr else "", ar = true)
            }
            if (s.layers.rashi && v.rashi.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(c.surfaceVariant).padding(10.dp)) {
                    Text("RASHI", style = MonoLabel, color = c.onSurfaceVariant)
                    TaggedText(rashiHtml(v.rashi), p.resolvers)
                }
            }
        }
    }
}

private val EMPTY_TEXT = mapOf(
    Commentary.RAMBAN to ("Ramban (Nachmanides)" to "Não disponível para esta parashá."),
    Commentary.OR_HACHAIM to ("Or HaChaim HaKadosh" to "Tradução de Devarim em andamento."),
    Commentary.BAAL_HATURIM to ("Kitzur Ba'al HaTurim" to "Tradução em andamento — poucas parashiot prontas."),
    Commentary.RASHI to ("Rashi" to "Comentário tagueado em pipeline."),
    Commentary.MIDRASH to ("Midrash Rabá" to "Não disponível para esta parashá."),
)

/** Each verse of the aliyá with the chosen commentary's comments on it (the site's EstudoCommentaryPane). */
private fun LazyListScope.commentaryPane(p: Parasha, s: ParashaState, verses: List<Verse>, comm: List<br.com.shiurhebraico.parasha.CommentAliyah>) {
    if (comm.isEmpty()) {
        item {
            if (!s.corporaLoaded) Empty("Carregando comentário…")
            else EMPTY_TEXT[s.commentary]?.let { (t, h) -> Empty("$t ainda não disponível para esta parashá.", h) }
        }
        return
    }
    val byKey = verses.associateBy { chapterVerse(it.ref)?.let { cv -> "${cv.first}:${cv.second}" } }
    val buckets = LinkedHashMap<String, MutableList<Comment>>()
    for (a in comm) for (cm in a.comments) {
        val k = "${cm.chapter}:${cm.verse}"
        buckets.getOrPut(k) { mutableListOf() }.add(cm)
    }
    val matched = buckets.filterKeys { byKey.containsKey(it) }
    val rows = (if (matched.isNotEmpty()) matched else buckets).entries.toList()
    items(rows, key = { "${s.commentary}/${it.key}" }) { (k, comments) ->
        val c = MaterialTheme.colorScheme
        val v = byKey[k]
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row {
                Text(v?.ref ?: "Cap. $k", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${comments.size} comentário${if (comments.size == 1) "" else "s"}", style = MonoLabel, color = c.onSurfaceVariant)
            }
            if (v != null) {
                if (s.layers.he && v.he.isNotEmpty()) Interlinear(heText(v.he, s.marks), if (s.layers.tr) v.tr else "")
                val pt = if (s.simple && v.ptSimple.isNotEmpty()) v.ptSimple else v.pt
                if (s.layers.pt && pt.isNotEmpty()) Text(pt, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, modifier = Modifier.padding(top = 6.dp))
            }
            comments.forEach { cm ->
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(c.surfaceVariant).padding(10.dp)) {
                    Row {
                        Text(s.commentary.label.uppercase(), style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                        cm.i?.let { Text("§$it", style = MonoLabel, color = c.onSurfaceVariant) }
                    }
                    TaggedText(cm.pt, p.resolvers)
                }
            }
        }
    }
}

/* ── Mitzvot ──────────────────────────────────────────────────────────── */

private fun LazyListScope.mitzvot(p: Parasha, vm: ParashaViewModel) {
    item {
        val pos = p.mitzvot.count { it.type.startsWith("pos") }
        Head("Mitzvot · Chinukh", "${p.mitzvot.size} mandamentos · $pos positivos · ${p.mitzvot.size - pos} negativos")
    }
    if (p.mitzvot.isEmpty()) item { Empty("Nenhuma mitzvá do Sefer HaChinuch nesta parashá.") }
    items(p.mitzvot, key = { "m${it.chinuch}/${it.titlePt}" }) { m ->
        val c = MaterialTheme.colorScheme
        val hasHalacha = p.halacha.any { it.chinuch == m.chinuch && it.refs.isNotEmpty() }
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.outline, RoundedCornerShape(8.dp)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${m.chinuch ?: "—"} · ${m.type.uppercase()}", style = MonoLabel, color = if (m.type.startsWith("pos")) Color(0xFF2E7D32) else Color(0xFFB23B3B), modifier = Modifier.weight(1f))
                Text(m.refPt, style = MonoLabel, color = c.onSurfaceVariant)
            }
            Text(m.titlePt, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            if (m.titleHe.isNotEmpty()) Text(m.titleHe, style = TextStyle(fontFamily = HebrewFamily, fontSize = 18.sp, textDirection = TextDirection.Rtl), color = c.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            if (m.verseText.isNotEmpty()) Text(m.verseText, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 17.sp, textDirection = TextDirection.Rtl), color = c.onSurface, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            if (m.description.isNotEmpty()) Text(m.description, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.padding(top = 6.dp))
            if (m.reason.isNotEmpty()) Text("Por quê: ${m.reason}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            val meta = listOfNotNull(m.talmud.ifEmpty { null }?.let { "Talmud: $it" }, m.appliesToday.ifEmpty { null }?.joinToString(", ")?.let { "Hoje: $it" })
            if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (hasHalacha) Text("HALACHÁ →", style = MonoLabel, color = c.primary, modifier = Modifier.padding(top = 8.dp).clickable { vm.goToHalacha(m.chinuch) })
        }
    }
}

/* ── Outras leituras: haftará, brit, tehilim ─────────────────────────── */

private fun LazyListScope.outras(p: Parasha, s: ParashaState) {
    item {
        val c = MaterialTheme.colorScheme
        Head("Outras Leituras", "— Leituras complementares da semana")
        val h = s.content?.haftara
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("HAFTARÁ", style = MonoLabel, color = c.onSurfaceVariant)
            Text(h?.ref?.ifEmpty { null } ?: p.haftaraRef.ifEmpty { "—" }, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            if (!h?.prophet.isNullOrEmpty()) Text(h!!.prophet, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            if (!h?.summary.isNullOrEmpty()) TaggedText(h!!.summary, p.resolvers)
            if (!h?.connection.isNullOrEmpty()) {
                Text("LIGAÇÃO COM A PARASHÁ", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                TaggedText(h!!.connection, p.resolvers)
            }
        }
        if (p.brit.isNotEmpty()) Column(Modifier.padding(vertical = 8.dp)) {
            Text("BRIT CHADASHÁ", style = MonoLabel, color = c.onSurfaceVariant)
            Text(p.brit, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        }
        if (p.tehilim.isNotEmpty()) Column(Modifier.padding(vertical = 8.dp)) {
            Text("TEHILIM", style = MonoLabel, color = c.onSurfaceVariant)
            Text("Salmo ${p.tehilim}", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        }
    }
}

/* ── Shabat: the times, the Mesa de Shabat, the blessings ────────────── */

private val BLESSINGS = listOf("Kiddush" to "noite-kidush", "Netilat Yadayim" to "refeicao-bencaos-dos-alimentos", "Hamotzi" to "refeicao-bencaos-dos-alimentos", "Birkat Hamazon" to "refeicao-bircat-hamazon")

private fun LazyListScope.shabat(s: ParashaState, onOpenSiddur: (String) -> Unit) {
    item {
        val c = MaterialTheme.colorScheme
        val t = remember { Zmanim.shabat() }
        val fmt = remember { SimpleDateFormat("EEE dd/MM · HH:mm", Locale("pt", "BR")).apply { timeZone = TimeZone.getTimeZone(DEFAULT_PLACE.tzid) } }
        Head("Shabat", "— Bênçãos, cantos e acendimento")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Velas" to t.candles, "Havdalá" to t.havdala).forEach { (k, d) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(c.surfaceVariant).padding(12.dp)) {
                    Text(k.uppercase(), style = MonoLabel, color = c.onSurfaceVariant)
                    Text(d?.let { fmt.format(it) } ?: "—", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                }
            }
        }
        Text(DEFAULT_PLACE.label + " · velas 18 min antes do pôr do sol, havdalá 30 min depois", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BLESSINGS.forEach { (label, slug) -> Chip(label.uppercase(), false) { onOpenSiddur(slug) } }
        }
        val m = s.content?.mesa
        Head("Mesa de Shabat")
        if (m == null) {
            Empty(if (s.corporaLoaded) "A Mesa de Shabat desta parashá ainda não está pronta." else "Carregando a Mesa de Shabat…")
        } else {
            if (m.dvarTorah.isNotEmpty()) { Text("DVAR TORÁ", style = MonoLabel, color = c.onSurfaceVariant); TaggedText(m.dvarTorah) }
            if (m.story.isNotEmpty()) { Text("CONTO", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)); TaggedText(m.story) }
            if (m.questions.isNotEmpty()) {
                Text("PERGUNTAS PARA A MESA", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                m.questions.forEachIndexed { i, q -> Text("${i + 1}. $q", style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            if (m.activity.isNotEmpty()) { Text("ATIVIDADE COM AS CRIANÇAS", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)); TaggedText(m.activity) }
        }
    }
}

/* ── Halachá: the Shulchan Aruch behind each mitzvá ──────────────────── */

private fun LazyListScope.halacha(p: Parasha, s: ParashaState, vm: ParashaViewModel) {
    val withRefs = p.halacha.filter { it.refs.isNotEmpty() }
    item { Head("Halachá · Shulchan Arukh", "Sefardita · Mechaber") }
    if (withRefs.isEmpty()) {
        item { Empty("Nenhuma halachá do Shulchan Aruch ligada às mitzvot desta parashá.") }
        return
    }
    item {
        Text("SELECIONE A MITZVÁ", style = MonoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(withRefs) { h -> Chip("#${h.chinuch}", h.chinuch == s.halacha) { vm.setHalacha(h.chinuch) } }
        }
    }
    val h = withRefs.firstOrNull { it.chinuch == s.halacha } ?: withRefs.first()
    item {
        val c = MaterialTheme.colorScheme
        Text(h.titlePt, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        if (h.sharedWith.isNotEmpty()) Text("Halachá compartilhada com " + h.sharedWith.joinToString(", ") { "#$it" }, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
    items(h.refs, key = { "${h.chinuch}/${it.ref}" }) { r ->
        val c = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.outline, RoundedCornerShape(8.dp)).padding(12.dp)) {
            Text(r.ref.uppercase(), style = MonoLabel, color = c.onSurfaceVariant)
            if (r.simanTopic.isNotEmpty()) Text(r.simanTopic, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
            if (r.he.isNotEmpty()) Text(heText(r.he, s.marks), style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 18.sp, lineHeight = 28.sp, textDirection = TextDirection.Rtl), color = c.onSurface, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            if (r.pt.isNotEmpty()) Text(r.pt, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
        }
    }
}

/* ── Midrash and Zohar: the translation alone (since 2026-09-16) ─────── */

private fun LazyListScope.midrash(p: Parasha, s: ParashaState) {
    val data = s.corpora.midrash ?: p.taggedMidrash
    item { Head("Midrash Rabbah", "Derashot por aliyá") }
    if (data.isEmpty()) {
        item { Empty(if (s.corporaLoaded) "Midrash Rabá ainda não disponível para esta parashá." else "Carregando…") }
        return
    }
    data.forEachIndexed { ai, a ->
        item(key = "ma$ai") { Text("${a.aliyah ?: ai + 1}ª ALIYÁ · ${a.comments.size} DERASHOT", style = MonoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp)) }
        items(a.comments, key = { "m$ai/${it.ref}/${it.i}" }) { d ->
            val c = MaterialTheme.colorScheme
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(6.dp)).background(c.surfaceVariant).padding(10.dp)) {
                Text(d.ref, style = MonoLabel, color = c.onSurfaceVariant)
                TaggedText(d.pt, p.resolvers)
            }
        }
    }
}

private fun LazyListScope.zohar(s: ParashaState) {
    val z = s.corpora.zohar
    item { Head("Zohar · זוהר", "Tradução em português") }
    if (z == null || z.sections.isEmpty()) {
        item { Empty(if (s.corporaLoaded) "O Zohar desta parashá ainda não está traduzido." else "Carregando…") }
        return
    }
    if (z.coverage.isNotEmpty()) item { Text(z.coverage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(z.sections, key = { it.ref }) { sec ->
        val c = MaterialTheme.colorScheme
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(sec.ref.uppercase(), style = MonoLabel, color = c.onSurfaceVariant)
            Text(sec.pt, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, maxLines = 400, overflow = TextOverflow.Ellipsis)
        }
    }
}
