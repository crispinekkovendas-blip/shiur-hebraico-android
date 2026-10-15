package br.com.shiurhebraico.parasha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.shiurhebraico.core.AppContainer
import br.com.shiurhebraico.core.hebrew.Marks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParashaListState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val books: List<Book> = emptyList(),
    val items: List<ParashaItem> = emptyList(),
    val current: String? = null,
)

class ParashaListViewModel(private val app: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ParashaListState())
    val state: StateFlow<ParashaListState> = _state

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = false) }
        launch { _state.update { it.copy(current = app.parashot.current()) } }
        try {
            val (books, items) = app.parashot.list()
            _state.update { it.copy(loading = false, books = books, items = items) }
        } catch (_: Exception) {
            _state.update { it.copy(loading = false, error = it.items.isEmpty()) }
        }
    }
}

/** the dashboard's sections, as the site's VALID_SECTIONS */
enum class PSection(val label: String) {
    INICIO("Início"), ESTUDO("Estudo"), MITZVOT("Mitzvot"), OUTRAS("Outras leituras"), SHABAT("Shabat"), HALACHA("Halachá"), MIDRASH("Midrash"), ZOHAR("Zohar")
}

enum class Commentary(val label: String) { RASHI("Rashi"), RAMBAN("Ramban"), OR_HACHAIM("Or HaChaim"), MIDRASH("Midrash"), BAAL_HATURIM("Ba'al HaTurim") }

data class PLayers(val he: Boolean = true, val tr: Boolean = true, val pt: Boolean = true, val ar: Boolean = false, val rashi: Boolean = false)

data class ParashaState(
    val slug: String,
    val loading: Boolean = true,
    val error: Boolean = false,
    val parasha: Parasha? = null,
    val corpora: Corpora = Corpora(),
    val corporaLoaded: Boolean = false,
    val section: PSection = PSection.INICIO,
    val aliyah: Int = 0,
    val commentary: Commentary = Commentary.RASHI,
    val layers: PLayers = PLayers(),
    val simple: Boolean = false,
    val marks: Marks = Marks(),
    val halacha: Int? = null,
) {
    val content: Content? get() = corpora.content ?: parasha?.content

    fun commentaryOf(k: Commentary): List<CommentAliyah> {
        val p = parasha ?: return emptyList()
        return when (k) {
            Commentary.RASHI -> p.taggedRashi
            Commentary.RAMBAN -> corpora.ramban ?: p.taggedRamban
            Commentary.OR_HACHAIM -> corpora.orHaChaim ?: p.taggedOrHaChaim
            Commentary.MIDRASH -> corpora.midrash ?: p.taggedMidrash
            Commentary.BAAL_HATURIM -> corpora.baalHaTurim.orEmpty()
        }
    }
}

class ParashaViewModel(private val app: AppContainer, slug: String, section: PSection) : ViewModel() {
    private val _state = MutableStateFlow(ParashaState(slug = slug, section = section, marks = app.prefs.marks))
    val state: StateFlow<ParashaState> = _state

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        val slug = _state.value.slug
        _state.update { it.copy(loading = true, error = false) }
        // the corpora (static files) load beside the API answer
        launch {
            val c = runCatching { app.parashot.loadCorpora(slug) }.getOrDefault(Corpora())
            _state.update { it.copy(corpora = c, corporaLoaded = true) }
        }
        try {
            val p = app.parashot.parasha(slug)
            _state.update { it.copy(parasha = p, loading = false, halacha = it.halacha ?: p.halacha.firstOrNull { h -> h.refs.isNotEmpty() }?.chinuch) }
        } catch (_: Exception) {
            _state.update { it.copy(loading = false, error = it.parasha == null) }
        }
    }

    fun setSection(s: PSection) = _state.update { it.copy(section = s) }
    fun setAliyah(i: Int) = _state.update { it.copy(aliyah = i) }
    fun jumpToAliyah(i: Int) = _state.update { it.copy(aliyah = i, section = PSection.ESTUDO) }
    fun setCommentary(k: Commentary) = _state.update { it.copy(commentary = k) }
    fun toggleLayer(k: String) = _state.update {
        val l = it.layers
        it.copy(layers = when (k) { "he" -> l.copy(he = !l.he); "tr" -> l.copy(tr = !l.tr); "pt" -> l.copy(pt = !l.pt); "ar" -> l.copy(ar = !l.ar); else -> l.copy(rashi = !l.rashi) })
    }
    fun setSimple(v: Boolean) = _state.update { it.copy(simple = v) }
    fun setMarks(m: Marks) = _state.update { app.prefs.marks = m; it.copy(marks = m) }
    fun goToHalacha(chinuch: Int?) = _state.update { it.copy(section = PSection.HALACHA, halacha = chinuch ?: it.halacha) }
    fun setHalacha(chinuch: Int?) = _state.update { it.copy(halacha = chinuch) }
}
