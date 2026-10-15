package br.com.shiurhebraico.core.hebrew

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The Alef-Bet and Nikud tables — app/src/main/assets/glossary.json, exported from the site's data files. */
@Serializable
data class GlossExample(val he: String = "", val tr: String = "", val pt: String = "")

@Serializable
data class LetterSound(val glyph: String = "", val tr: String = "", val sound: String = "", val example: GlossExample = GlossExample())

@Serializable
data class FinalForm(val glyph: String = "", val name: String = "")

@Serializable
data class Mnemonic(val shape: String = "", val meaning: String = "", val hook: String = "")

@Serializable
data class Letter(
    val key: String,
    val glyph: String = "",
    val name: String = "",
    val nameHe: String = "",
    val value: Int = 0,
    val sounds: List<LetterSound> = emptyList(),
    val final: FinalForm? = null,
    val note: String? = null,
    val mnemonic: Mnemonic = Mnemonic(),
)

@Serializable
data class NikudSign(
    val key: String,
    val group: String = "",
    val write: String = "",
    val glyph: String = "",
    val name: String = "",
    val nameHe: String = "",
    val tr: String = "",
    val sound: String = "",
    val example: GlossExample? = null,
    val note: String? = null,
)

@Serializable
data class GlossaryData(val alefBet: List<Letter> = emptyList(), val nikud: List<NikudSign> = emptyList())

object Glossary {
    @Volatile
    var data: GlossaryData = GlossaryData()
        private set

    fun load(text: String) {
        data = Json { ignoreUnknownKeys = true }.decodeFromString(GlossaryData.serializer(), text)
    }
}
