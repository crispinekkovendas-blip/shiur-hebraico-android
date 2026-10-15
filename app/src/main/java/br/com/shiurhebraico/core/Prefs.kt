package br.com.shiurhebraico.core

import android.content.Context
import br.com.shiurhebraico.core.hebrew.Marks
import br.com.shiurhebraico.siddur.LastPrayer

/** The Siddur's and the Parashá's remembered choices. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("estudo", Context.MODE_PRIVATE)

    var readMode: String
        get() = sp.getString("siddurReadMode", "ESTUDO") ?: "ESTUDO"
        set(v) = sp.edit().putString("siddurReadMode", v).apply()

    /** Prática: no nikud, letters-only transliteration */
    var practice: Boolean
        get() = sp.getBoolean("siddurPractice", false)
        set(v) = sp.edit().putBoolean("siddurPractice", v).apply()

    /** the nikud / te'amim switches, shared by the Siddur and the Parashá */
    var marks: Marks
        get() = Marks(sp.getBoolean("marksNikud", true), sp.getBoolean("marksTeamim", true))
        set(v) = sp.edit().putBoolean("marksNikud", v.nikud).putBoolean("marksTeamim", v.teamim).apply()

    fun lastPrayer(): LastPrayer? {
        val slug = sp.getString("siddurLastSlug", null) ?: return null
        return LastPrayer(slug, sp.getString("siddurLastPrayer", "") ?: "", sp.getString("siddurLastService", "") ?: "")
    }

    fun saveLastPrayer(l: LastPrayer) = sp.edit().putString("siddurLastSlug", l.slug).putString("siddurLastPrayer", l.prayer).putString("siddurLastService", l.service).apply()
}
