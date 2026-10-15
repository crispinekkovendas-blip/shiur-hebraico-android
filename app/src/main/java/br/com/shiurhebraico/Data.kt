package br.com.shiurhebraico

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** The website is the backend: the same catalogue, study files and subtitles the site reads. */
const val BASE = "https://shiur-hebraico.vercel.app"

@Serializable
data class Book(val key: String, val namePt: String, val trad: String = "")

@Serializable
data class Parasha(val key: String, val name: String, val he: String = "", val tr: String = "", val book: String = "")

@Serializable
data class Moed(val key: String, val name: String, val he: String = "")

@Serializable
data class Rav(
    val slug: String,
    val name: String,
    val he: String = "",
    val photo: String = "",
    val bio: String = "",
    val trad: String = "",
    val canal: Boolean = false,
)

/** one lesson, the catalogue's short field names (see W05 scripts/build-catalog.mjs) */
@Serializable
data class Lesson(
    val id: String,
    val h: String = "",
    val t: String = "",
    val tp: String = "",
    val r: String = "",
    val d: Int = 0,
    val p: String = "",
    val y: Int = 0,
    val k: String = "",
    val pa: String = "",
    val mo: String = "",
    val m: Int = 0,
    val q: Int = 0,
    val s: Int = 0,
    val sum: String = "",
) {
    val title get() = h.ifBlank { tp.ifBlank { t } }
    val thumb get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

@Serializable
data class Catalog(
    val books: List<Book> = emptyList(),
    val parashiot: List<Parasha> = emptyList(),
    val moadim: List<Moed> = emptyList(),
    val traditions: Map<String, String> = emptyMap(),
    val week: String = "",
    val ravs: List<Rav> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
) {
    val ravBySlug by lazy { ravs.associateBy { it.slug } }
    val parashaByKey by lazy { parashiot.associateBy { it.key } }
    val lessonById by lazy { lessons.associateBy { it.id } }
    val byRav by lazy { lessons.groupBy { it.r } }
    val byParasha by lazy { lessons.filter { it.pa.isNotBlank() }.groupBy { it.pa } }

    fun placeOf(l: Lesson): String = when {
        l.pa.isNotBlank() -> parashaByKey[l.pa]?.name ?: ""
        l.mo.isNotBlank() -> moadim.firstOrNull { it.key == l.mo }?.name ?: ""
        l.k == "tefila" -> "Tefilot"
        else -> "Outros temas"
    }

    fun tradName(r: Rav?): String = r?.trad?.let { traditions[it] } ?: ""

    /** a course's modules: the five books, Moadim, Tefilot, Outros temas — Torah order inside */
    fun modulesOf(ls: List<Lesson>): List<Pair<String, List<Lesson>>> {
        val order = parashiot.mapIndexed { i, p -> p.key to i }.toMap()
        val parts = books.map { it.key to it.namePt } + listOf("moadim" to "Moadim", "tefila" to "Tefilot", "outros" to "Outros temas")
        val known = parts.map { it.first }.toSet()
        return parts.mapNotNull { (key, name) ->
            val xs = ls.filter { it.k == key || (key == "outros" && it.k !in known) }
                .sortedWith(compareBy<Lesson> { order[it.pa] ?: 999 }.thenBy { it.mo }.thenByDescending { it.p })
            if (xs.isEmpty()) null else name to xs
        }
    }
}

data class Cue(val start: Double, val end: Double, val he: String)

data class Subs(val cues: List<Cue>, val pt: List<String>?, val en: List<String>?)

data class Moment(val t: Double, val label: String)
data class Question(val t: Double, val q: String, val a: String)
data class Source(val t: Double, val kind: String, val ref: String, val text: String)
data class Study(val summary: String, val moments: List<Moment>, val questions: List<Question>, val sources: List<Source>)

val SOURCE_LABELS = linkedMapOf(
    "tanakh" to "Tanach",
    "talmud" to "Talmud",
    "midrash" to "Midrash",
    "halacha" to "Halachá",
    "zohar" to "Zohar e Cabalá",
    "comentario" to "Comentaristas",
    "tefila" to "Tefilá e Sidur",
    "outro" to "Outras fontes",
)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

private fun JsonElement.num(): Double = (this as? JsonPrimitive)?.doubleOrNull ?: 0.0
private fun JsonElement.str(): String = if (this is JsonNull) "" else (this as? JsonPrimitive)?.content ?: ""

class Repo(private val ctx: Context) {
    private val http = OkHttpClient.Builder()
        .cache(Cache(File(ctx.cacheDir, "http"), 40L * 1024 * 1024))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()
    private val catalogFile = File(ctx.filesDir, "catalog.json")

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(BASE + path).header("x-shiur-app", "1").build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            r.body!!.string()
        }
    }

    /** the catalogue saved by the last visit — the app opens at once, then refreshes */
    suspend fun cachedCatalog(): Catalog? = withContext(Dispatchers.IO) {
        runCatching { if (catalogFile.exists()) json.decodeFromString<Catalog>(catalogFile.readText()) else null }.getOrNull()
    }

    suspend fun freshCatalog(): Catalog {
        val text = get("/app/catalogo/")
        val c = json.decodeFromString<Catalog>(text)
        withContext(Dispatchers.IO) { runCatching { catalogFile.writeText(text) } }
        return c
    }

    suspend fun study(id: String): Study {
        val o = json.parseToJsonElement(get("/estudo/$id.json")).jsonObject
        val moments = o["moments"]?.jsonArray?.map { val a = it.jsonArray; Moment(a[0].num(), a.getOrNull(1)?.str() ?: "") } ?: emptyList()
        val questions = o["questions"]?.jsonArray?.map {
            val q = it.jsonObject
            Question(q["t"]?.num() ?: 0.0, q["q"]?.str() ?: "", q["a"]?.str() ?: "")
        } ?: emptyList()
        val sources = o["sources"]?.jsonArray?.map {
            val a = it.jsonArray
            Source(a[0].num(), a.getOrNull(1)?.str() ?: "outro", a.getOrNull(2)?.str() ?: "", a.getOrNull(3)?.str() ?: "")
        } ?: emptyList()
        return Study(o["summary"]?.str() ?: "", moments, questions, sources)
    }

    suspend fun subs(id: String): Subs {
        val o = json.parseToJsonElement(get("/legendas/?v=$id")).jsonObject
        val cues = o["cues"]!!.jsonArray.map { val a = it.jsonArray; Cue(a[0].num(), a[1].num(), a[2].str()) }
        fun texts(k: String) = (o[k] as? JsonArray)?.map { it.str() }?.takeIf { it.size == cues.size }
        return Subs(cues, texts("pt"), texts("en"))
    }
}

/** where each lesson was left, and which were opened last ("Continuar assistindo") */
class Progress(ctx: Context) {
    private val sp = ctx.getSharedPreferences("progress", Context.MODE_PRIVATE)
    fun position(id: String): Float = sp.getFloat("pos_$id", 0f)
    fun duration(id: String): Float = sp.getFloat("dur_$id", 0f)
    fun save(id: String, at: Float, dur: Float) {
        val recent = (listOf(id) + recent().filter { it != id }).take(30)
        sp.edit().putFloat("pos_$id", at).putFloat("dur_$id", dur).putString("recent", recent.joinToString(",")).apply()
    }
    fun recent(): List<String> = sp.getString("recent", "")!!.split(',').filter { it.isNotBlank() }
    var lang: String
        get() = sp.getString("lang", "pt")!!
        set(v) = sp.edit().putString("lang", v).apply()
    var dub: Boolean
        get() = sp.getBoolean("dub", false)
        set(v) = sp.edit().putBoolean("dub", v).apply()
    var askedNotify: Boolean
        get() = sp.getBoolean("askedNotify", false)
        set(v) = sp.edit().putBoolean("askedNotify", v).apply()
}

/** Rosh HaShaná of each year: the Hebrew year a lesson was uploaded in */
private val RH = listOf(
    5784 to "2023-09-16", 5785 to "2024-10-03", 5786 to "2025-09-23", 5787 to "2026-09-12", 5788 to "2027-10-02", 5789 to "2028-09-21",
)

fun thisHebYear(): Int {
    val today = LocalDate.now().toString()
    return RH.lastOrNull { today >= it.second }?.first ?: 5787
}

fun duration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}min" else "$m min"
}

fun clock(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val x = s % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${x.toString().padStart(2, '0')}" else "$m:${x.toString().padStart(2, '0')}"
}

/** the cue playing at t: the last one that started at or before t (binary search) */
fun cueAt(cues: List<Cue>, t: Double): Int {
    var lo = 0
    var hi = cues.size - 1
    var best = -1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (cues[mid].start <= t) { best = mid; lo = mid + 1 } else hi = mid - 1
    }
    return if (best >= 0 && t <= cues[best].end + 1.5) best else -1
}

fun initials(name: String) = name.replace(Regex("^(Rav|Rabino|Rabbi|Canal)\\s+"), "").split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
