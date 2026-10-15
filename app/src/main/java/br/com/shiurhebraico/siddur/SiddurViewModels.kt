package br.com.shiurhebraico.siddur

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.shiurhebraico.core.AppContainer
import br.com.shiurhebraico.core.Zmanim
import br.com.shiurhebraico.core.hebrew.Marks
import br.com.shiurhebraico.videos.sidur.labelPt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

/** the six categories, in the cover's order, with their letters (SiddurSidebar's CAT_LETTERS) */
val CAT_LETTERS = linkedMapOf("dia-a-dia" to "א", "shabat" to "ב", "bencaos" to "ג", "calendario" to "ד", "jejuns-e-luto" to "ה", "estudo" to "ו")

fun catKey(label: String) = Normalizer.normalize(label, Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036F]"), "").lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-')

/** the service Agora points at, per category (the site's NOW_SERVICE) */
private val NOW_SERVICE = mapOf(
    "dia-a-dia" to mapOf("shacharit" to "Shacharit: Tefila", "mincha" to "Mincha", "arvit" to "Arvit"),
    "shabat" to mapOf("shacharit" to "Shacharit", "mincha" to "Tarde", "arvit" to "Arvit"),
)

data class SvcGroup(val label: String, val items: List<SiddurSection>)
data class CatGroup(val key: String, val label: String, val letter: String, val services: List<SvcGroup>) {
    val count get() = services.sumOf { it.items.size }
}

data class LastPrayer(val slug: String, val prayer: String, val service: String)

data class SiddurHomeState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val sections: List<SiddurSection> = emptyList(),
    val cats: List<CatGroup> = emptyList(),
    val nowLabel: String = "",
    val nowSlug: String = "",
    val last: LastPrayer? = null,
    val cat: String = "",
    val svc: String = "",
    val search: String = "",
)

class SiddurHomeViewModel(private val app: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SiddurHomeState())
    val state: StateFlow<SiddurHomeState> = _state

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = false, last = app.prefs.lastPrayer()) }
        try {
            val sections = app.siddur.index()
            val cats = mutableListOf<CatGroup>()
            for (s in sections) {
                val k = catKey(s.category)
                val i = cats.indexOfFirst { it.key == k }
                if (i < 0) cats.add(CatGroup(k, s.category, CAT_LETTERS[k] ?: "ס", listOf(SvcGroup(s.service, listOf(s)))))
                else {
                    val g = cats[i]
                    val j = g.services.indexOfFirst { it.label == s.service }
                    val svcs = g.services.toMutableList()
                    if (j < 0) svcs.add(SvcGroup(s.service, listOf(s))) else svcs[j] = svcs[j].copy(items = svcs[j].items + s)
                    cats[i] = g.copy(services = svcs)
                }
            }
            val order = CAT_LETTERS.keys.toList()
            cats.sortBy { order.indexOf(it.key).let { o -> if (o < 0) 99 else o } }
            // Agora: the service for this moment (zmanim), on Shabat the Shabat book
            val svc = Zmanim.serviceNow()
            val shabbat = Zmanim.isShabbat()
            val cat = if (shabbat) "shabat" else "dia-a-dia"
            val slug = sections.firstOrNull { catKey(it.category) == cat && it.service == NOW_SERVICE[cat]?.get(svc) }?.slug ?: ""
            _state.update {
                it.copy(
                    loading = false, sections = sections, cats = cats, nowSlug = slug,
                    nowLabel = (if (shabbat) "Shabat · " else "") + Zmanim.serviceLabel(svc),
                )
            }
        } catch (_: Exception) {
            _state.update { it.copy(loading = false, error = it.sections.isEmpty()) }
        }
    }

    fun refreshLast() = _state.update { it.copy(last = app.prefs.lastPrayer()) }
    fun openCat(k: String) = _state.update { it.copy(cat = if (it.cat == k) "" else k, svc = "") }
    fun openSvc(k: String) = _state.update { it.copy(svc = if (it.svc == k) "" else k) }
    fun setSearch(q: String) = _state.update { it.copy(search = q) }

    /** the search, as the sidebar's: prayer, heading, service, category, opening words */
    fun results(s: SiddurHomeState): List<SiddurSection> {
        val q = fold(s.search)
        if (q.isEmpty()) return emptyList()
        return s.sections.filter { x -> listOf(x.prayer, x.heading, labelPt(x.service), labelPt(x.category), x.incipitTr, x.incipitHe).any { fold(it).contains(q) } }.take(60)
    }

    private fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036F\\u0591-\\u05C7]"), "").lowercase().trim()
}

/* ── the reader ───────────────────────────────────────────────────────── */

enum class ReadMode(val label: String) { ESTUDO("Estudo"), HEBRAICO("Hebraico"), PORTUGUES("Português") }

data class Layers(val he: Boolean = true, val tr: Boolean = true, val pt: Boolean = true)

data class ReaderState(
    val slug: String,
    val part: Int = 0,
    val page: SiddurPage? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val mode: ReadMode = ReadMode.ESTUDO,
    val layers: Layers = Layers(),
    /** Prática: Hebrew without nikud and the transliteration of the letters only */
    val practice: Boolean = false,
    val marks: Marks = Marks(),
    val rtl: Boolean = true,
    val openNotes: Set<Int> = emptySet(),
    val siblings: List<SiddurSection> = emptyList(),
)

class SiddurReaderViewModel(private val app: AppContainer, slug: String, part: Int) : ViewModel() {
    private val _state = MutableStateFlow(
        ReaderState(
            slug = slug, part = part,
            mode = runCatching { ReadMode.valueOf(app.prefs.readMode) }.getOrDefault(ReadMode.ESTUDO),
            practice = app.prefs.practice, marks = app.prefs.marks,
        ),
    )
    val state: StateFlow<ReaderState> = _state

    init {
        load()
        viewModelScope.launch { _state.update { it.copy(siblings = runCatching { app.siddur.index() }.getOrDefault(emptyList())) } }
    }

    fun load() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(loading = true, error = false) }
        try {
            val page = app.siddur.page(s.slug, s.part)
            _state.update { it.copy(page = page, loading = false) }
            app.prefs.saveLastPrayer(LastPrayer(page.section.slug, page.section.prayer, page.section.service))
        } catch (_: Exception) {
            _state.update { it.copy(loading = false, error = true) }
        }
    }

    fun setPart(p: Int) {
        if (p == _state.value.part) return
        _state.update { it.copy(part = p, openNotes = emptySet()) }
        load()
    }

    fun go(slug: String) {
        _state.update { it.copy(slug = slug, part = 0, page = null, openNotes = emptySet()) }
        load()
    }

    fun setMode(m: ReadMode) {
        app.prefs.readMode = m.name
        _state.update { it.copy(mode = m) }
    }

    fun toggleLayer(k: String) = _state.update {
        it.copy(layers = when (k) { "he" -> it.layers.copy(he = !it.layers.he); "tr" -> it.layers.copy(tr = !it.layers.tr); else -> it.layers.copy(pt = !it.layers.pt) })
    }

    fun togglePractice() = _state.update { val v = !it.practice; app.prefs.practice = v; it.copy(practice = v) }
    fun setMarks(m: Marks) = _state.update { app.prefs.marks = m; it.copy(marks = m) }
    fun setRtl(v: Boolean) = _state.update { it.copy(rtl = v) }
    fun toggleNote(i: Int) = _state.update { it.copy(openNotes = if (i in it.openNotes) it.openNotes - i else it.openNotes + i) }
}
