package br.com.shiurhebraico

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Dublagem (beta, 2026-10-15): the phone's own voice reads each translated line when it starts, while
 * the rabbi's voice is turned down under it. Free and offline (Google's pt-BR / English voices).
 * A line is spoken faster when its text is long for its slot; when the voice falls behind, what is
 * waiting is dropped so the dub never runs late.
 */
class Dubber(ctx: Context) {
    private var ready = false
    private val queued = AtomicInteger(0)
    private var n = 0
    private val tts: TextToSpeech = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { queued.updateAndGet { (it - 1).coerceAtLeast(0) } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { queued.updateAndGet { (it - 1).coerceAtLeast(0) } }
        })
    }

    /** false when the phone has no voice for the language (Settings → Text-to-speech) */
    fun setLang(lang: String): Boolean {
        if (!ready) return true
        val r = tts.setLanguage(if (lang == "en") Locale.US else Locale("pt", "BR"))
        return r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /** speak a line that has `slot` seconds until the next one starts */
    fun say(text: String, slot: Double) {
        if (!ready || text.isBlank()) return
        val behind = queued.get() > 0
        // about 15 characters a second at rate 1.0 (Portuguese and English alike)
        var rate = (text.length / slot.coerceAtLeast(0.8) / 15.0).toFloat().coerceIn(1.0f, 1.7f)
        if (behind) rate = (rate + 0.15f).coerceAtMost(1.8f)
        tts.setSpeechRate(rate)
        // one line speaking + this one waiting is fine; a second one waiting means we are late: drop them
        val mode = if (queued.get() >= 2) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        if (mode == TextToSpeech.QUEUE_FLUSH) queued.set(0)
        queued.incrementAndGet()
        tts.speak(text, mode, Bundle(), "l${n++}")
    }

    fun stop() {
        queued.set(0)
        if (ready) tts.stop()
    }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }
}
