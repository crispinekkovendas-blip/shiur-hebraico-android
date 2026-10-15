package br.com.shiurhebraico.siddur

import br.com.shiurhebraico.core.net.SiteClient
import br.com.shiurhebraico.core.net.arr
import br.com.shiurhebraico.core.net.bool
import br.com.shiurhebraico.core.net.num
import br.com.shiurhebraico.core.net.obj
import br.com.shiurhebraico.core.net.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Shapes of /api/siddur (the site's types/siddur.ts). */
data class SiddurWord(val idx: Int, val he: String, val tr: String, val pt: String)

/** A rubric: words only the chazan says, words the kahal answers, or words said only on an occasion. */
data class SiddurSpan(
    val from: Int,
    val to: Int,
    val kind: String,
    val occ: String? = null,
    val who: String? = null,
    val pt: String = "",
    val he: String = "",
    val omit: Boolean = false,
)

data class SiddurVerse(
    val index: Int,
    val isHeader: Boolean,
    val labelPt: String,
    val notesPt: String,
    val translationPt: String,
    val words: List<SiddurWord>,
    val spans: List<SiddurSpan>,
)

data class SiddurPart(val index: Int, val heading: String, val wordCount: Int)

data class SiddurSection(
    val slug: String,
    val category: String,
    val service: String,
    val prayer: String,
    val sortOrder: Int,
    val verseCount: Int,
    val wordCount: Int,
    val partCount: Int,
    val incipitHe: String,
    val incipitTr: String,
    val heading: String,
    val prev: String?,
    val next: String?,
    val parts: List<SiddurPart> = emptyList(),
)

data class SiddurPage(val section: SiddurSection, val part: Int, val verses: List<SiddurVerse>)

/* ── the rubric helpers (utils/siddur-roles.ts) ───────────────────────── */

private fun size(s: SiddurSpan) = s.to - s.from

/** the span a word belongs to — the innermost when spans nest */
fun spanAt(spans: List<SiddurSpan>, idx: Int): SiddurSpan? {
    var best: SiddurSpan? = null
    for (s in spans) {
        if (idx < s.from || idx > s.to) continue
        if (best == null || size(s) < size(best)) best = s
    }
    return best
}

/** the spans that begin at a word, the widest first: their pills go before that word */
fun spanStarts(spans: List<SiddurSpan>, idx: Int): List<SiddurSpan> = spans.filter { it.from == idx }.sortedByDescending { size(it) }

/** true when the span is the whole verse rather than an insertion inside it */
fun coversVerse(span: SiddurSpan, words: List<SiddurWord>): Boolean =
    words.isNotEmpty() && span.from <= words.first().idx && span.to >= words.last().idx

private val COMBINED = Regex("^\\[combined with (\\d+)\\]$", RegexOption.IGNORE_CASE)

/** the Portuguese of the words in a span (markers and gaps skipped) */
fun insertPt(words: List<SiddurWord>, span: SiddurSpan): String = words
    .filter { it.idx in span.from..span.to }
    .map { it.pt.trim() }
    .filter { it.isNotEmpty() && !COMBINED.matches(it) }
    .joinToString(" ")

/** what the pill says: the occasion (or that the verse is left out then), or who speaks, in Hebrew */
fun pillText(span: SiddurSpan): String = if (span.kind == "occasion") (if (span.omit) "Não se diz em ${span.pt}" else span.pt) else span.he

/** the kinds present in a part, in reading order */
fun legendOf(verses: List<SiddurVerse>): List<String> {
    val out = mutableListOf<String>()
    for (v in verses) for (s in v.spans) if (s.kind !in out) out.add(s.kind)
    return out
}

val LEGEND = mapOf(
    "chazan" to "Chazan — só quem lidera diz",
    "kahal" to "Kahal — o público responde",
    "occasion" to "Só na ocasião indicada",
)

/* ── phrase groups for the Estudo interlinear (utils/siddur-groups.ts) ── */

data class WordGroup(val words: List<SiddurWord>, val pt: String)

fun groupWords(words: List<SiddurWord>): List<WordGroup> {
    val byIdx = HashMap<Int, Int>()
    words.forEachIndexed { i, w -> byIdx[w.idx] = i }
    val root = IntArray(words.size) { it }
    fun find(x: Int): Int {
        var i = x
        while (root[i] != i) {
            root[i] = root[root[i]]
            i = root[i]
        }
        return i
    }
    words.forEachIndexed { i, w ->
        val m = COMBINED.find(w.pt) ?: return@forEachIndexed
        val j = byIdx[m.groupValues[1].toInt()] ?: return@forEachIndexed
        root[find(i)] = find(j)
    }
    val groups = mutableListOf<WordGroup>()
    val shown = HashSet<Int>()
    var curR = -1
    var cur = mutableListOf<SiddurWord>()
    fun flush() {
        val rootPt = words[curR].pt
        val pt = if (COMBINED.matches(rootPt)) "" else rootPt
        if (shown.contains(curR)) groups.add(WordGroup(cur, ""))
        else {
            shown.add(curR)
            groups.add(WordGroup(cur, pt))
        }
    }
    words.forEachIndexed { i, w ->
        val r = find(i)
        if (curR < 0 || curR != r) {
            if (curR >= 0) flush()
            curR = r
            cur = mutableListOf()
        }
        cur.add(w)
    }
    if (curR >= 0) flush()
    return groups
}

/* ── the API ──────────────────────────────────────────────────────────── */

/** The Siddur from the site's /api/siddur: the index once, each prayer part on demand, kept on the phone. */
class SiddurRepository(private val site: SiteClient, private val dir: File?) {
    private val parts = ConcurrentHashMap<String, SiddurPage>()
    @Volatile private var index: List<SiddurSection>? = null

    suspend fun index(): List<SiddurSection> {
        index?.let { return it }
        val text = try {
            site.text("/api/siddur?section=index").also { t -> withContext(Dispatchers.IO) { dir?.let { File(it, "siddur-index.json").writeText(t) } } }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { dir?.let { File(it, "siddur-index.json") }?.takeIf { it.exists() }?.readText() } ?: throw e
        }
        val list = site.json.parseToJsonElement(text).jsonObject.arr("sections").orEmpty().map { sectionOf(it.jsonObject) }
        index = list
        return list
    }

    suspend fun page(slug: String, part: Int): SiddurPage {
        val key = "$slug#$part"
        parts[key]?.let { return it }
        val cache = dir?.let { File(File(it, "siddur").apply { mkdirs() }, "${slug}_$part.json") }
        val text = try {
            site.text("/api/siddur?section=${URLEncoder.encode(slug, "UTF-8")}${if (part > 0) "&part=$part" else ""}")
                .also { t -> withContext(Dispatchers.IO) { cache?.writeText(t) } }
        } catch (e: Exception) {
            // a prayer opened before reads offline
            withContext(Dispatchers.IO) { cache?.takeIf { it.exists() }?.readText() } ?: throw e
        }
        val j = site.json.parseToJsonElement(text).jsonObject
        val page = SiddurPage(
            section = sectionOf(j.obj("section") ?: JsonObject(emptyMap())),
            part = j.num("part")?.toInt() ?: part,
            verses = j.arr("verses").orEmpty().map { verseOf(it.jsonObject) },
        )
        parts[key] = page
        return page
    }

    private fun sectionOf(o: JsonObject) = SiddurSection(
        slug = o.str("slug") ?: "",
        category = o.str("category") ?: "",
        service = o.str("service") ?: "",
        prayer = o.str("prayer") ?: "",
        sortOrder = o.num("sortOrder")?.toInt() ?: 0,
        verseCount = o.num("verseCount")?.toInt() ?: 0,
        wordCount = o.num("wordCount")?.toInt() ?: 0,
        partCount = o.num("partCount")?.toInt() ?: 1,
        incipitHe = o.str("incipitHe") ?: "",
        incipitTr = o.str("incipitTr") ?: "",
        heading = o.str("heading") ?: "",
        prev = o.str("prev"),
        next = o.str("next"),
        parts = o.arr("parts").orEmpty().map { val p = it.jsonObject; SiddurPart(p.num("index")?.toInt() ?: 0, p.str("heading") ?: "", p.num("wordCount")?.toInt() ?: 0) },
    )

    private fun verseOf(o: JsonObject) = SiddurVerse(
        index = o.num("index")?.toInt() ?: 0,
        isHeader = o.bool("isHeader") ?: false,
        labelPt = o.str("labelPt") ?: "",
        notesPt = o.str("notesPt") ?: "",
        translationPt = o.str("translationPt") ?: "",
        words = o.arr("words").orEmpty().map { val w = it.jsonObject; SiddurWord(w.num("idx")?.toInt() ?: 0, w.str("he") ?: "", w.str("tr") ?: "", w.str("pt") ?: "") },
        spans = o.arr("spans").orEmpty().map {
            val s = it.jsonObject
            SiddurSpan(s.num("from")?.toInt() ?: 0, s.num("to")?.toInt() ?: 0, s.str("kind") ?: "", s.str("occ"), s.str("who"), s.str("pt") ?: "", s.str("he") ?: "", s.bool("omit") ?: false)
        },
    )
}
