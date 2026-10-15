package br.com.shiurhebraico.parasha

import br.com.shiurhebraico.core.net.HttpError
import br.com.shiurhebraico.core.net.SiteClient
import br.com.shiurhebraico.core.net.arr
import br.com.shiurhebraico.core.net.asNum
import br.com.shiurhebraico.core.net.asStr
import br.com.shiurhebraico.core.net.num
import br.com.shiurhebraico.core.net.obj
import br.com.shiurhebraico.core.net.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** The Parashá's shapes (the site's types/parasha.ts), read loosely from /api/parasha and /data/. */
data class Book(val key: String, val namePt: String, val trad: String, val number: Int)
data class ParashaItem(val key: String, val name: String, val hebrewName: String, val traducao: String, val book: String, val orderIndex: Int)

data class Verse(val ref: String, val he: String, val tr: String, val pt: String, val ptSimple: String, val ar: String, val arTr: String, val rashi: String)
data class Aliyah(val nameHe: String, val namePt: String, val nameTranslit: String, val refPt: String, val verses: List<Verse>)

/** one comment of a commentary, anchored to a verse; `pt` may carry tagged HTML */
data class Comment(val ref: String, val i: Int?, val chapter: Int?, val verse: Int?, val pt: String, val bookHe: String)
data class CommentAliyah(val aliyah: Int?, val comments: List<Comment>)

data class Mitzvah(
    val chinuch: Int?, val type: String, val titlePt: String, val titleHe: String, val refPt: String, val refHe: String,
    val description: String, val reason: String, val talmud: String, val appliesToday: List<String>, val category: List<String>, val verseText: String,
)

data class SaRef(val book: String, val ref: String, val siman: Int?, val seif: Int?, val simanTopic: String, val simanTitleHe: String, val pt: String, val he: String)
data class HalachaEntry(val chinuch: Int?, val titlePt: String, val sharedWith: List<Int>, val refs: List<SaRef>)

data class Haftara(val ref: String, val prophet: String, val connection: String, val summary: String)
data class Mesa(val questions: List<String>, val story: String, val activity: String, val dvarTorah: String)
data class ContentAliyah(val num: Int?, val name: String, val verses: String, val summary: String)
data class Comentario(val source: String, val ref: String, val title: String, val body: String)
data class Content(val resumo: String, val aliyot: List<ContentAliyah>, val comentarios: List<Comentario>, val haftara: Haftara?, val mesa: Mesa?)

data class ZoharSection(val ref: String, val he: String, val pt: String)
data class Zohar(val coverage: String, val sections: List<ZoharSection>)

data class Resolver(val key: String, val kind: String, val displayPt: String, val displayHe: String, val names: List<String>, val bio: String)

data class Parasha(
    val key: String,
    val name: String,
    val hebrewName: String,
    val traducao: String,
    val tagline: String,
    val bookName: String,
    val tora: String,
    val haftaraRef: String,
    val brit: String,
    val tehilim: String,
    val resumo: String,
    val estudo: String,
    val temas: List<String>,
    val aliyot: List<Aliyah>,
    val mitzvot: List<Mitzvah>,
    val halacha: List<HalachaEntry>,
    val taggedRashi: List<CommentAliyah>,
    val taggedRamban: List<CommentAliyah>,
    val taggedOrHaChaim: List<CommentAliyah>,
    val taggedMidrash: List<CommentAliyah>,
    /** curated content embedded in the API answer (haftará, Mesa de Shabat, comentários) */
    val content: Content?,
    val resolvers: Map<String, Resolver>,
)

data class Corpora(
    val ramban: List<CommentAliyah>? = null,
    val orHaChaim: List<CommentAliyah>? = null,
    val baalHaTurim: List<CommentAliyah>? = null,
    val midrash: List<CommentAliyah>? = null,
    val zohar: Zohar? = null,
    val content: Content? = null,
)

private fun JsonElement?.refString(): String = when (this) {
    is JsonPrimitive -> asStr() ?: ""
    is JsonObject -> str("ref_pt") ?: str("ref") ?: str("text") ?: ""
    else -> ""
}

private fun JsonObject.strings(k: String) = arr(k)?.mapNotNull { it.asStr() }.orEmpty()

/** "Devarim 32:1" / "Deuteronomy 32:1" → (32, 1) */
fun chapterVerse(ref: String?): Pair<Int, Int>? {
    val m = Regex("(\\d+):(\\d+)\\s*$").find(ref ?: "") ?: return null
    return m.groupValues[1].toInt() to m.groupValues[2].toInt()
}

class ParashaRepository(private val site: SiteClient, private val dir: File?) {
    private val parashot = ConcurrentHashMap<String, Parasha>()
    private val corpora = ConcurrentHashMap<String, Any>()

    suspend fun list(): Pair<List<Book>, List<ParashaItem>> {
        val o = cachedObj("/api/parashiot", "parashiot.json")
        val books = o.arr("books").orEmpty().map { val b = it.jsonObject; Book(b.str("key") ?: "", b.str("namePt") ?: "", b.str("trad") ?: "", b.num("number")?.toInt() ?: 0) }
        val items = o.arr("parashiot").orEmpty().map {
            val p = it.jsonObject
            ParashaItem(p.str("key") ?: "", p.str("name") ?: "", p.str("hebrewName") ?: "", p.str("traducao") ?: "", p.str("book") ?: "", p.num("orderIndex")?.toInt() ?: 0)
        }
        return books to items
    }

    suspend fun current(): String? = try { site.obj("/api/home").obj("currentParasha")?.str("slug") } catch (_: Exception) { null }

    suspend fun parasha(slug: String): Parasha {
        parashot[slug]?.let { return it }
        val o = cachedObj("/api/parasha?slug=${enc(slug)}", "parasha-$slug.json")
        val p = o.obj("parasha") ?: JsonObject(emptyMap())
        val rc = o.obj("rc") ?: JsonObject(emptyMap())
        val out = Parasha(
            key = p.str("key") ?: slug,
            name = p.str("name") ?: slug,
            hebrewName = p.str("hebrewName") ?: p.str("nameHebrew") ?: "",
            traducao = p.str("traducao") ?: "",
            tagline = p.str("tagline") ?: "",
            bookName = p.str("bookName") ?: "",
            tora = p.str("tora") ?: "",
            haftaraRef = p["haftara"].refString(),
            brit = p["brit"].refString().ifEmpty { rc["brit"].refString() },
            tehilim = p.str("tehilim") ?: "",
            resumo = rc.str("resumo") ?: p.str("resumo") ?: "",
            estudo = rc.str("estudo") ?: "",
            temas = p.strings("temas"),
            aliyot = rc.arr("aliyot").orEmpty().map { aliyahOf(it.jsonObject) },
            mitzvot = rc.arr("mitzvot").orEmpty().map { mitzvahOf(it.jsonObject) },
            halacha = rc.arr("mitzvotHalacha").orEmpty().map { halachaOf(it.jsonObject) },
            taggedRashi = commentaryOf(rc.obj("taggedRashi")),
            taggedRamban = commentaryOf(rc.obj("taggedRamban")),
            taggedOrHaChaim = commentaryOf(rc.obj("taggedOrHaChaim")),
            taggedMidrash = commentaryOf(rc.obj("taggedMidrash")),
            content = contentOf(rc),
            resolvers = rc.obj("resolverKeys")?.mapValues { (k, v) ->
                val r = v.jsonObject
                Resolver(r.str("key") ?: k, r.str("kind") ?: "", r.str("display_pt") ?: k, r.str("display_he") ?: "", r.strings("names"), r.str("bio_pt") ?: "")
            }.orEmpty(),
        )
        parashot[slug] = out
        return out
    }

    /** a static corpus (/data/commentary/<corpus>/<slug>.json) — null when this parashá has none */
    suspend fun corpus(corpus: String, slug: String): JsonObject? {
        val key = "$corpus/$slug"
        (corpora[key] as? JsonObject)?.let { return it }
        val path = if (corpus == "content") "/data/parasha-content/${enc(slug)}.json" else "/data/commentary/$corpus/${enc(slug)}.json"
        return try {
            cachedObj(path, "c-$corpus-$slug.json").also { corpora[key] = it }
        } catch (e: HttpError) {
            if (e.code == 404) null else throw e
        }
    }

    suspend fun loadCorpora(slug: String): Corpora {
        suspend fun comm(c: String) = corpus(c, slug)?.let { commentaryOf(it) }
        return Corpora(
            ramban = comm("ramban"),
            orHaChaim = comm("or-hachaim"),
            baalHaTurim = comm("baal-haturim"),
            midrash = corpus("midrash", slug)?.let(::midrashOf),
            zohar = corpus("zohar", slug)?.let { z -> Zohar(z.str("coverage") ?: "", z.arr("sections").orEmpty().map { val s = it.jsonObject; ZoharSection(s.str("ref") ?: "", s.str("he") ?: "", s.str("pt") ?: "") }) },
            content = corpus("content", slug)?.let(::contentOf),
        )
    }

    private suspend fun cachedObj(path: String, file: String): JsonObject {
        val f = dir?.let { File(File(it, "parasha").apply { mkdirs() }, file) }
        val text = try {
            site.text(path).also { t -> withContext(Dispatchers.IO) { f?.writeText(t) } }
        } catch (e: Exception) {
            if (e is HttpError && e.code == 404) throw e
            withContext(Dispatchers.IO) { f?.takeIf { it.exists() }?.readText() } ?: throw e
        }
        return site.json.parseToJsonElement(text).jsonObject
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun aliyahOf(a: JsonObject) = Aliyah(
        nameHe = a.str("name_he") ?: "",
        namePt = a.str("name_pt") ?: "",
        nameTranslit = a.str("name_translit") ?: "",
        refPt = a.str("ref_pt") ?: a.str("ref") ?: "",
        verses = a.arr("verses").orEmpty().map {
            val v = it.jsonObject
            Verse(v.str("ref") ?: "", v.str("he") ?: "", v.str("tr") ?: "", v.str("pt") ?: "", v.str("ptSimple") ?: "", v.str("ar") ?: "", v.str("arTr") ?: "", v.str("rashi") ?: "")
        },
    )

    private fun commentOf(c: JsonObject) = Comment(
        ref = c.str("ref") ?: "",
        i = c.num("i")?.toInt(),
        chapter = c["chapter"].asNum()?.toInt() ?: c["chapter"].asStr()?.toIntOrNull(),
        verse = c["verse"].asNum()?.toInt() ?: c["verse"].asStr()?.toIntOrNull(),
        pt = c.str("pt_tagged") ?: c.str("pt") ?: c.str("pt_translation") ?: "",
        bookHe = c.str("book_he") ?: "",
    )

    private fun commentaryOf(o: JsonObject?): List<CommentAliyah> = o?.arr("aliyot").orEmpty().map {
        val a = it.jsonObject
        CommentAliyah(a["aliyah"].asNum()?.toInt() ?: a["aliyah"].asStr()?.toIntOrNull(), (a.arr("comments") ?: a.arr("derashot")).orEmpty().map { c -> derashahOrComment(c.jsonObject) })
    }

    /** a derashah as a comment: anchored on its anchor verse (the site's derashotAsComments) */
    private fun derashahOrComment(c: JsonObject): Comment {
        val base = commentOf(c)
        if (base.chapter != null && base.verse != null && c.str("anchor_verse") == null) return base
        val cv = chapterVerse(c.str("anchor_verse")) ?: (base.chapter to base.verse).let { (a, b) -> if (a != null && b != null) a to b else null }
        return base.copy(chapter = cv?.first, verse = cv?.second, i = base.i ?: c.num("n")?.toInt())
    }

    private fun midrashOf(o: JsonObject): List<CommentAliyah> = o.arr("aliyot").orEmpty().map {
        val a = it.jsonObject
        CommentAliyah(a["aliyah"].asNum()?.toInt(), a.arr("derashot").orEmpty().map { d -> derashahOrComment(d.jsonObject) })
    }

    private fun mitzvahOf(m: JsonObject) = Mitzvah(
        chinuch = m.num("chinuch")?.toInt(), type = m.str("type") ?: "", titlePt = m.str("title_pt") ?: "", titleHe = m.str("title_he") ?: "",
        refPt = m.str("ref_pt") ?: "", refHe = m.str("ref_he") ?: "", description = m.str("description") ?: "", reason = m.str("reason") ?: "",
        talmud = m.str("talmud") ?: "", appliesToday = m.strings("applies_today"), category = m.strings("category"), verseText = m.str("verse_text") ?: "",
    )

    private fun halachaOf(h: JsonObject) = HalachaEntry(
        chinuch = h.num("chinuch")?.toInt(),
        titlePt = h.str("title_pt") ?: "",
        sharedWith = h.arr("shared_with").orEmpty().mapNotNull { it.asNum()?.toInt() },
        refs = h.arr("sa_refs").orEmpty().map {
            val r = it.jsonObject
            SaRef(r.str("book") ?: "", r.str("ref") ?: "", r.num("siman")?.toInt(), r.num("seif")?.toInt(), r.str("siman_topic_pt") ?: "", r.str("siman_title_he") ?: "", r.str("pt") ?: r.str("text_pt") ?: "", r.str("he_nikkud") ?: r.str("he_plain") ?: "")
        },
    )

    private fun contentOf(o: JsonObject): Content? {
        val haf = o.obj("haftara")
        val mesa = o.obj("mesa_de_shabat")
        val coms = o.arr("comentarios")
        if (haf == null && mesa == null && coms == null && o.arr("aliyot") == null) return null
        return Content(
            resumo = o.str("resumo") ?: "",
            aliyot = o.arr("aliyot").orEmpty().mapNotNull { e ->
                val a = e as? JsonObject ?: return@mapNotNull null
                if (a.containsKey("verses") && a["verses"] is JsonArray) return@mapNotNull null // the API's verse aliyot, not summaries
                ContentAliyah(a.num("num")?.toInt(), a.str("name") ?: "", a.str("verses") ?: "", a.str("summary") ?: "")
            },
            comentarios = coms.orEmpty().map { val c = it.jsonObject; Comentario(c.str("source") ?: "", c.str("ref") ?: "", c.str("title") ?: "", c.str("body") ?: "") },
            haftara = haf?.let { Haftara(it.str("ref") ?: "", it.str("prophet") ?: "", it.str("connection") ?: "", it.str("summary") ?: "") },
            mesa = mesa?.let { Mesa(it.strings("questions"), it.str("story") ?: "", it.str("activity") ?: "", it.str("dvar_torah") ?: "") },
        )
    }
}
