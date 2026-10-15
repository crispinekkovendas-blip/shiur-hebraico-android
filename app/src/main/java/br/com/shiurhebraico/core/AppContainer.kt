package br.com.shiurhebraico.core

import android.content.Context
import br.com.shiurhebraico.core.net.SiteClient
import br.com.shiurhebraico.parasha.ParashaRepository
import br.com.shiurhebraico.siddur.SiddurRepository

/**
 * The Torah study and the Siddur (2026-10-15), ported from the Emunah Shlemah app: they read the
 * main site (emunah-shlemah-qwik) — /api/parasha, /api/siddur and the static corpora.
 */
class AppContainer(context: Context) {
    val site = SiteClient(context.cacheDir)
    val prefs = Prefs(context)
    val siddur = SiddurRepository(site, context.filesDir)
    val parashot = ParashaRepository(site, context.filesDir)
}
