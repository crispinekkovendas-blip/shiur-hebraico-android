package br.com.shiurhebraico

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** what a screen can ask the app to open */
class Nav(
    val lesson: (String) -> Unit,
    val rav: (String) -> Unit,
    val parasha: (String) -> Unit,
    val back: () -> Unit,
)

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = p.ink) } else Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ── Início ───────────────────────────────────────────────────────────────────── */

@Composable
fun HomeScreen(c: Catalog, progress: Progress, nav: Nav) {
    val p = LocalPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val week = c.parashaByKey[c.week]
    val weekLessons = remember(c) { c.byParasha[c.week].orEmpty().sortedByDescending { it.p } }
    // the library by Hebrew year: "Novo! 5787" (since Rosh HaShaná) first, then every year it has
    val yearCounts = remember(c) { c.lessons.filter { it.y > 0 }.groupingBy { it.y }.eachCount() }
    val years = remember(c) { yearCounts.keys.sortedDescending() }
    var year by rememberSaveable { mutableIntStateOf(0) }
    val shownYear = if (year == 0) years.firstOrNull() ?: 0 else year
    val ofYear = remember(c, shownYear) { c.lessons.filter { it.y == shownYear }.sortedByDescending { it.p } }
    val recent = remember(c) { progress.recent().mapNotNull { c.lessonById[it] }.take(10) }
    val topRavs = remember(c) { c.ravs.sortedByDescending { c.byRav[it.slug]?.size ?: 0 }.take(16) }
    val hits = remember(c, query) {
        val q = query.trim().lowercase()
        if (q.length < 2) emptyList() else c.lessons.filter {
            it.title.lowercase().contains(q) || it.t.contains(query.trim()) || it.tp.lowercase().contains(q) ||
                (c.ravBySlug[it.r]?.name?.lowercase()?.contains(q) == true) || c.placeOf(it).lowercase().contains(q)
        }.take(60)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text("Shiur Hebraico", style = MaterialTheme.typography.displaySmall, color = p.ink)
                Text("Aulas de Torá em hebraico, com legendas em português.", style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 4.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Buscar aula, mestre ou parashá") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = p.surface, unfocusedContainerColor = p.surface, unfocusedBorderColor = p.line),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
        }
        if (query.trim().length >= 2) {
            item { SectionTitle("${hits.size} resultados") }
            items(hits, key = { "s" + it.id }) { l -> LessonRow(c, l, onOpen = nav.lesson) }
            return@LazyColumn
        }
        if (week != null) {
            item {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(p.tekhelet).clickable { nav.parasha(week.key) }.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HebrewText(week.he, 38, p.onTekhelet, weight = FontWeight.Bold)
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("PARASHÁ DA SEMANA", style = MaterialTheme.typography.labelSmall, color = p.onTekhelet.copy(alpha = 0.8f))
                        Text(week.name, style = MaterialTheme.typography.headlineSmall, color = p.onTekhelet)
                        Text(week.tr, style = MaterialTheme.typography.bodySmall, color = p.onTekhelet.copy(alpha = 0.85f))
                    }
                    Text("${weekLessons.size}\naulas", style = MaterialTheme.typography.labelLarge, color = p.onTekhelet, textAlign = TextAlign.Center)
                }
            }
            if (weekLessons.isNotEmpty()) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(weekLessons.take(12), key = { "w" + it.id }) { l -> LessonCard(c, l, 260.dp, onOpen = nav.lesson) }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("Continuar assistindo") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recent, key = { "r" + it.id }) { l ->
                        val d = progress.duration(l.id).takeIf { it > 0 } ?: l.d.toFloat()
                        LessonCard(c, l, 220.dp, progress.position(l.id) / d.coerceAtLeast(1f), nav.lesson)
                    }
                }
            }
        }
        item { SectionTitle("Mestres") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(topRavs, key = { "m" + it.slug }) { r ->
                    Column(Modifier.width(92.dp).clip(RoundedCornerShape(10.dp)).clickable { nav.rav(r.slug) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Portrait(r, 72.dp)
                        Text(r.name, style = MaterialTheme.typography.labelMedium, color = p.ink, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        c.tradName(r).takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = p.tekhelet, maxLines = 1) }
                    }
                }
            }
        }
        item { SectionTitle("Por ano") }
        stickyHeader(key = "years") {
            Column(Modifier.background(p.paper)) {
                YearTabs(years, shownYear, yearCounts, { year = it })
                HorizontalDivider(color = p.line)
            }
        }
        item(key = "yhead$shownYear") {
            Text(
                if (shownYear == thisHebYear()) "${ofYear.size} aulas novas desde Rosh HaShaná $shownYear" else "${ofYear.size} aulas de $shownYear",
                style = MaterialTheme.typography.bodySmall,
                color = p.muted,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp),
            )
        }
        items(ofYear, key = { "y" + it.id }) { l -> Box(Modifier.animateItemPlacement()) { LessonRow(c, l, onOpen = nav.lesson) } }
    }
}

/* ── Mestres ──────────────────────────────────────────────────────────────────── */

@Composable
fun MestresScreen(c: Catalog, nav: Nav) {
    val p = LocalPalette.current
    val groups = remember(c) {
        val keys = c.traditions.keys.toList()
        val byTrad = c.ravs.groupBy { if (it.trad in keys) it.trad else "" }
        (keys.map { c.traditions[it]!! to byTrad[it].orEmpty() } + listOf("Outros" to byTrad[""].orEmpty()))
            .map { (name, rs) -> name to rs.sortedByDescending { c.byRav[it.slug]?.size ?: 0 } }
            .filter { it.second.isNotEmpty() }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text("Mestres", style = MaterialTheme.typography.displaySmall, color = p.ink)
                Text("${c.ravs.size} mestres e canais, por tradição.", style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
        groups.forEach { (name, rs) ->
            item(key = "g$name") { SectionTitle(name) }
            items(rs, key = { "t" + it.slug }) { r ->
                val n = c.byRav[r.slug]?.size ?: 0
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface)
                        .border(1.dp, p.line, RoundedCornerShape(12.dp)).clickable { nav.rav(r.slug) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Portrait(r, 56.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, color = p.ink, maxLines = 2)
                        if (r.he.isNotBlank()) HebrewText(r.he, 15, p.muted)
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            c.tradName(r).takeIf { it.isNotBlank() }?.let { Tag(it) }
                            if (r.canal) Tag("Canal")
                        }
                    }
                    Text("$n\naulas", style = MaterialTheme.typography.labelMedium, color = p.muted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/* ── Parashiot ────────────────────────────────────────────────────────────────── */

@Composable
fun ParashiotScreen(c: Catalog, nav: Nav) {
    val p = LocalPalette.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text("Parashiot", style = MaterialTheme.typography.displaySmall, color = p.ink)
                Text("As 54 porções da Torá, livro por livro.", style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
        c.books.forEach { b ->
            val ps = c.parashiot.filter { it.book == b.key }
            item(key = "b" + b.key) { SectionTitle("${b.namePt} · ${b.trad}") }
            ps.chunked(2).forEach { pair ->
                item(key = "p" + pair.first().key) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { x ->
                            val n = c.byParasha[x.key]?.size ?: 0
                            val week = x.key == c.week
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (week) p.tekhelet else p.surface)
                                    .border(1.dp, if (week) p.tekhelet else p.line, RoundedCornerShape(12.dp)).clickable { nav.parasha(x.key) }.padding(12.dp),
                            ) {
                                HebrewText(x.he, 21, if (week) p.onTekhelet else p.ink, weight = FontWeight.Bold)
                                Text(x.name, style = MaterialTheme.typography.titleSmall, color = if (week) p.onTekhelet else p.ink)
                                Text(if (n == 0) "sem aulas ainda" else "$n aulas", style = MaterialTheme.typography.bodySmall, color = if (week) p.onTekhelet.copy(alpha = 0.85f) else p.muted)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        val moadim = c.moadim.filter { m -> c.lessons.any { it.mo == m.key } }
        if (moadim.isNotEmpty()) {
            item { SectionTitle("Moadim") }
            item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    moadim.forEach { m ->
                        Text(
                            "${m.name} · ${c.lessons.count { it.mo == m.key }}",
                            style = MaterialTheme.typography.labelLarge,
                            color = p.tekhelet,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(p.wash).clickable { nav.parasha("moed:" + m.key) }.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}

/* ── a parashá (or a moed): its lessons, newest first ─────────────────────────── */

@Composable
fun ParashaScreen(c: Catalog, key: String, nav: Nav) {
    val p = LocalPalette.current
    val moed = key.startsWith("moed:")
    val mk = key.removePrefix("moed:")
    val par = if (moed) null else c.parashaByKey[key]
    val m = if (moed) c.moadim.firstOrNull { it.key == mk } else null
    val ls = remember(c, key) { (if (moed) c.lessons.filter { it.mo == mk } else c.byParasha[key].orEmpty()).sortedByDescending { it.p } }
    val idx = c.parashiot.indexOfFirst { it.key == key }
    Column(Modifier.fillMaxSize()) {
        TopBar(par?.name ?: m?.name ?: "", nav.back)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    HebrewText(par?.he ?: m?.he ?: "", 40, p.ink, weight = FontWeight.Bold)
                    Text(par?.tr ?: "", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                    Text("${ls.size} aulas", style = MaterialTheme.typography.labelLarge, color = p.tekhelet, modifier = Modifier.padding(top = 6.dp))
                }
                HorizontalDivider(color = p.line)
            }
            if (ls.isEmpty()) item { Text("Ainda não há aulas com legendas para esta parashá.", color = p.muted, modifier = Modifier.padding(16.dp)) }
            items(ls, key = { it.id }) { l -> LessonRow(c, l, onOpen = nav.lesson) }
            if (!moed && idx >= 0) {
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        c.parashiot.getOrNull(idx - 1)?.let { x -> Text("‹ ${x.name}", color = p.tekhelet, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { nav.parasha(x.key) }.padding(8.dp)) } ?: Spacer(Modifier.width(1.dp))
                        c.parashiot.getOrNull(idx + 1)?.let { x -> Text("${x.name} ›", color = p.tekhelet, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { nav.parasha(x.key) }.padding(8.dp)) }
                    }
                }
            }
        }
    }
}

/* ── a teacher's course: modules by book ──────────────────────────────────────── */

@Composable
fun RavScreen(c: Catalog, slug: String, progress: Progress, nav: Nav) {
    val p = LocalPalette.current
    val r = c.ravBySlug[slug] ?: return
    val ls = c.byRav[slug].orEmpty()
    // the modules can be narrowed to one Hebrew year ("Todos" = every year)
    val ravYearCounts = remember(c, slug) { ls.filter { it.y > 0 }.groupingBy { it.y }.eachCount() }
    val ravYears = remember(c, slug) { ravYearCounts.keys.sortedDescending() }
    var ravYear by rememberSaveable(slug) { mutableIntStateOf(0) }
    val modules = remember(c, slug, ravYear) { c.modulesOf(if (ravYear == 0) ls else ls.filter { it.y == ravYear }) }
    var bioOpen by rememberSaveable { mutableStateOf(false) }
    val open = remember { mutableStateOf(setOf(modules.firstOrNull()?.first ?: "")) }
    val first = remember(c, slug) { progress.recent().firstNotNullOfOrNull { id -> ls.firstOrNull { it.id == id } } ?: modules.firstOrNull()?.second?.firstOrNull() }
    Column(Modifier.fillMaxSize()) {
        TopBar(r.name, nav.back)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Portrait(r, 104.dp)
                    Text(r.name, style = MaterialTheme.typography.headlineSmall, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    if (r.he.isNotBlank()) HebrewText(r.he, 18, p.muted)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        c.tradName(r).takeIf { it.isNotBlank() }?.let { Tag(it) }
                        if (r.canal) Tag("Canal")
                    }
                    if (r.bio.isNotBlank()) {
                        Text(
                            r.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.ink,
                            maxLines = if (bioOpen) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 12.dp).animateContentSize().clickable { bioOpen = !bioOpen },
                        )
                        if (!bioOpen) Text("Ler mais", style = MaterialTheme.typography.labelLarge, color = p.tekhelet, modifier = Modifier.align(Alignment.Start).clickable { bioOpen = true }.padding(vertical = 4.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Fact("${ls.size}", "aulas")
                        Fact("${ls.sumOf { it.d } / 3600}", "horas")
                        Fact("${modules.size}", "módulos")
                        Fact("${ls.sumOf { it.s }}", "fontes")
                    }
                    if (first != null) {
                        Row(
                            Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.tekhelet).clickable { nav.lesson(first.id) }.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = p.onTekhelet)
                            Text(if (progress.position(first.id) > 5) "Continuar o curso" else "Começar o curso", color = p.onTekhelet, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            if (ravYears.size > 1) {
                stickyHeader(key = "ravyears") {
                    Column(Modifier.background(p.paper).padding(top = 10.dp)) {
                        YearTabs(ravYears, ravYear, ravYearCounts, { y ->
                            ravYear = y
                            // a chosen year opens its modules at once
                            if (y != 0) open.value = c.modulesOf(ls.filter { it.y == y }).map { it.first }.toSet()
                        }, all = true)
                        HorizontalDivider(color = p.line)
                    }
                }
            }
            modules.forEach { (name, xs) ->
                val isOpen = name in open.value
                item(key = "m$name") {
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface)
                            .border(1.dp, p.line, RoundedCornerShape(12.dp))
                            .clickable { open.value = if (isOpen) open.value - name else open.value + name }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleLarge, color = p.ink)
                            Text("${xs.size} aulas · ${duration(xs.sumOf { it.d })}", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        }
                        Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = p.muted)
                    }
                }
                if (isOpen) {
                    xs.forEachIndexed { i, l ->
                        item(key = "l$name${l.id}") {
                            val d = progress.duration(l.id).takeIf { it > 0 } ?: l.d.toFloat()
                            Box(Modifier.animateItemPlacement()) { LessonRow(c, l, number = i + 1, showRav = false, progress = progress.position(l.id) / d.coerceAtLeast(1f), onOpen = nav.lesson) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(n: String, label: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = p.ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.muted)
    }
}

@Composable
fun Centered(text: String) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = p.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun ChipRow(items: List<Pair<String, Boolean>>, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, (label, on) ->
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (on) p.onTekhelet else p.tekhelet,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) p.tekhelet else p.wash).clickable { onClick(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
