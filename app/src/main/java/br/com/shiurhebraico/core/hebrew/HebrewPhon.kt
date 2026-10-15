package br.com.shiurhebraico.core.hebrew

import java.text.Normalizer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Phonetic analysis of one pointed Hebrew word in Brazilian Portuguese terms — a port of the site's
 * utils/hebrew-phon.ts (letter by letter, syllables, stress, encounters, the reading rules that
 * fired). Sephardic pronunciation, the Siddur's transliteration conventions. When the engine's
 * transliteration is given it settles stress, kamats gadol/katan and vocal/silent shva.
 */

private object M {
    const val shva = 'ְ'
    const val chatafSegol = 'ֱ'
    const val chatafPatach = 'ֲ'
    const val chatafKamats = 'ֳ'
    const val chirik = 'ִ'
    const val tsere = 'ֵ'
    const val segol = 'ֶ'
    const val patach = 'ַ'
    const val kamats = 'ָ'
    const val cholam = 'ֹ'
    const val cholamVav = 'ֺ'
    const val kubuts = 'ֻ'
    const val dagesh = 'ּ'
    const val meteg = 'ֽ'
    const val maqaf = '־'
    const val shinDot = 'ׁ'
    const val sinDot = 'ׂ'
    const val kamatsKatan = 'ׇ'
}

private val VOWEL_OF = mapOf(
    M.kamats to "kamats", M.kamatsKatan to "kamats-katan", M.patach to "patach", M.chatafPatach to "chataf-patach",
    M.tsere to "tsere", M.segol to "segol", M.chatafSegol to "chataf-segol", M.chirik to "chirik",
    M.cholam to "cholam", M.cholamVav to "cholam", M.kubuts to "kubuts", M.chatafKamats to "chataf-kamats",
)
private val SOUND_OF = mapOf(
    "kamats" to "a", "kamats-katan" to "o", "patach" to "a", "chataf-patach" to "a", "tsere" to "ê", "segol" to "e",
    "chataf-segol" to "e", "chirik" to "i", "cholam" to "o", "chataf-kamats" to "o", "kubuts" to "u", "shuruk" to "u",
    "shva-na" to "e", "shva-nach" to "",
)
private val LONG = setOf("kamats", "tsere", "cholam", "shuruk")
private val BEGADKEFAT = setOf('ב', 'ג', 'ד', 'כ', 'ך', 'פ', 'ף', 'ת')
private val FINAL_TO_BASE = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')
private val LETTER_KEY = mapOf(
    'א' to "alef", 'ב' to "bet", 'ג' to "guimel", 'ד' to "dalet", 'ה' to "he", 'ו' to "vav", 'ז' to "zayin", 'ח' to "chet", 'ט' to "tet", 'י' to "yod",
    'כ' to "kaf", 'ל' to "lamed", 'מ' to "mem", 'נ' to "nun", 'ס' to "samech", 'ע' to "ayin", 'פ' to "pe", 'צ' to "tsade", 'ק' to "kof", 'ר' to "resh", 'ש' to "shin", 'ת' to "tav",
)
private val CONS = mapOf(
    'א' to "", 'ב' to "v", 'ג' to "g", 'ד' to "d", 'ה' to "h", 'ו' to "v", 'ז' to "z", 'ח' to "ch", 'ט' to "t", 'י' to "y", 'כ' to "ch", 'ל' to "l", 'מ' to "m", 'נ' to "n",
    'ס' to "s", 'ע' to "", 'פ' to "f", 'צ' to "ts", 'ק' to "k", 'ר' to "r", 'ש' to "sh", 'ת' to "t",
)
private val HARD = mapOf('ב' to "b", 'כ' to "k", 'פ' to "p")
private val KATAN_STEMS = listOf("חכמ", "צהר", "אכל", "אזנ")

class PhonUnit(
    val glyph: String,
    val base: Char,
    val isFinal: Boolean,
    val letterName: String,
    var cons: String,
    var vowel: String?,
    var vowelSound: String = "",
    var role: String = "consoante",
    var dagesh: String? = null,
    var shin: String? = null,
    val meteg: Boolean = false,
    var ganuv: Boolean = false,
    val notes: MutableList<String> = mutableListOf(),
    var tr: String = "",
    var syllable: Int = -1,
)

class PhonSyllable(
    var he: String = "",
    var tr: String = "",
    val units: MutableList<Int> = mutableListOf(),
    var open: Boolean = true,
    var stressed: Boolean = false,
    var nucleus: String = "monotongo",
    var nucleusText: String = "",
)

data class PhonEncounter(val kind: String, val text: String, val where: String, val desc: String)

class PhonAnalysis(
    val he: String,
    val tr: String,
    val trSyllables: String,
    val units: List<PhonUnit>,
    val syllables: List<PhonSyllable>,
    val stress: Int,
    val count: String,
    val tonicity: String,
    val encounters: List<PhonEncounter>,
    val rules: List<String>,
    val divineName: Boolean,
    val guided: Boolean,
)

private class RawUnit(val letter: Char, val marks: MutableList<Char> = mutableListOf()) {
    fun has(c: Char) = c in marks
}

private val TAAMIM = Regex("[\\u0591-\\u05AF]")
fun stripTaamim(s: String): String = s.replace(TAAMIM, "")

private fun tokenize(word: String): List<RawUnit> {
    val units = mutableListOf<RawUnit>()
    for (ch in stripTaamim(Normalizer.normalize(word, Normalizer.Form.NFKD))) {
        val c = ch.code
        if (c in 0x05D0..0x05EA) units.add(RawUnit(ch))
        else if (c in 0x05B0..0x05BD || c == 0x05BF || c == 0x05C1 || c == 0x05C2 || c == 0x05C4 || c == 0x05C5 || c == 0x05C7) {
            units.lastOrNull()?.marks?.add(ch)
        }
    }
    return units
}

private val DIVINE = Regex("^(יְהֹוָה|יְהוָה|יהוה|יְיָ|יְיׇ|יי|ה׳|ה')$")
private val PUNCT = Regex("[׃.,;:!?()\\[\\]\"'׳״]")
private val ALL_MARKS = Regex("[\\u0591-\\u05C7]")

fun analyzeWord(heRaw: String, trRaw: String = ""): PhonAnalysis {
    val he = stripTaamim(Normalizer.normalize(heRaw, Normalizer.Form.NFKD)).replace(PUNCT, "").trim()
    val rules = mutableListOf<String>()
    val rule = { s: String -> if (s !in rules) rules.add(s) }
    val bare = he.replace(ALL_MARKS, "")
    val divineName = DIVINE.matches(he.replace("ֽ", "")) || DIVINE.matches(bare)

    val segments = he.split(M.maqaf).filter { tokenize(it).isNotEmpty() }
    val units = mutableListOf<PhonUnit>()
    for ((si, seg) in segments.withIndex()) {
        val segUnits = buildUnits(tokenize(seg), rule)
        if (si > 0) rule("Maqaf (־): as palavras unidas pelo hífen leem-se como uma só, com um único acento no fim.")
        units.addAll(segUnits)
    }

    var guided = false
    if (trRaw.isNotEmpty() && !divineName) guided = applyGuidance(units, trRaw, rule)

    val syllables = syllabify(units)
    val stress = pickStress(units, syllables, trRaw, guided)
    syllables.forEachIndexed { i, s -> s.stressed = i == stress }

    for (s in syllables) {
        val parts = s.units.map { units[it].tr }.toMutableList()
        if (s.stressed && syllables.size > 1) {
            val k = s.units.indexOfFirst { units[it].vowelSound.isNotEmpty() && units[it].role != "semivogal" }
            if (k >= 0) {
                val u = units[s.units[k]]
                val consLen = if (u.ganuv) 0 else u.cons.length
                parts[k] = u.tr.substring(0, min(consLen, u.tr.length)) + accentuate(u.tr.substring(min(consLen, u.tr.length)))
            }
        }
        s.tr = parts.joinToString("")
        s.he = s.units.joinToString("") { units[it].glyph }
    }
    val trSyllables = syllables.joinToString("·") { it.tr }

    val n = syllables.size
    val count = when (n) { 1 -> "monossílaba"; 2 -> "dissílaba"; 3 -> "trissílaba"; else -> "polissílaba" }
    val fromEnd = n - 1 - stress
    val tonicity = if (n == 1) "monossílaba tônica" else when (fromEnd) { 0 -> "oxítona"; 1 -> "paroxítona"; else -> "proparoxítona" }

    val encounters = findEncounters(units, syllables)
    if (divineName) rule("Nome divino: por reverência não se pronuncia como está escrito; lê-se Adonai (ou HaShem fora da oração).")

    return PhonAnalysis(
        he = he,
        tr = trRaw.ifEmpty { syllables.joinToString("") { it.tr } },
        trSyllables = trSyllables,
        units = units,
        syllables = syllables,
        stress = stress,
        count = count,
        tonicity = tonicity,
        encounters = encounters,
        rules = rules,
        divineName = divineName,
        guided = guided,
    )
}

private fun letterName(base: Char): String = Glossary.data.alefBet.firstOrNull { it.key == LETTER_KEY[base] }?.name ?: base.toString()

private val VOWEL_NAME = mapOf(
    "kamats" to "kamats gadol", "kamats-katan" to "kamats katan", "patach" to "patach", "chataf-patach" to "chataf patach",
    "tsere" to "tsere", "segol" to "segol", "chataf-segol" to "chataf segol", "chirik" to "chirik", "cholam" to "cholam",
    "chataf-kamats" to "chataf kamats", "kubuts" to "kubuts", "shuruk" to "shuruk", "shva-na" to "shva na", "shva-nach" to "shva nach",
)

private fun vowelDesc(v: String): String = Glossary.data.nikud.firstOrNull { it.key == v }?.sound ?: ""

private fun buildUnits(raw: List<RawUnit>, rule: (String) -> Unit): List<PhonUnit> {
    val out = mutableListOf<PhonUnit>()
    fun hasVowelMark(r: RawUnit) = r.marks.any { VOWEL_OF[it] != null || it == M.shva }
    fun vowelKeyOf(r: RawUnit): String? {
        for (m in r.marks) VOWEL_OF[m]?.let { return it }
        return null
    }

    fun finish(u: PhonUnit): PhonUnit {
        u.tr = if (u.role == "consoante muda" && u.vowelSound == "") "" else if (u.ganuv) "a" + u.cons else u.cons + u.vowelSound
        return u
    }

    /** a mater gives its vowel to the consonant before it (יוֹם → yod carries "o"); word-initial maters keep it */
    fun lendVowel(u: PhonUnit, key: String, sound: String) {
        val p = out.lastOrNull()
        if (p != null && p.vowelSound.isEmpty() && (p.role == "consoante" || p.role == "consoante muda" || p.role == "portadora de vogal")) {
            p.vowel = key
            p.vowelSound = sound
            if (p.role == "consoante muda") p.role = "portadora de vogal"
            if (p.base == 'י' && p.role == "consoante") p.cons = "i"
            p.tr = p.cons + sound
            p.notes.add("Recebe a vogal \"$sound\" da letra seguinte (${if (key == "shuruk") "shuruk" else "cholam male"}).")
            u.vowel = null
            u.vowelSound = ""
        } else {
            u.vowel = key
            u.vowelSound = sound
        }
    }

    for (i in raw.indices) {
        val r = raw[i]
        val prev = raw.getOrNull(i - 1)
        val next = raw.getOrNull(i + 1)
        val isFirst = i == 0
        val isLast = i == raw.size - 1
        val base = FINAL_TO_BASE[r.letter] ?: r.letter
        val isFinal = FINAL_TO_BASE.containsKey(r.letter)
        val dag = r.has(M.dagesh)
        val shva = r.has(M.shva)
        val vk = vowelKeyOf(r)
        val u = PhonUnit(
            glyph = r.letter + r.marks.joinToString(""),
            base = base,
            isFinal = isFinal,
            letterName = letterName(base),
            cons = CONS[base] ?: "",
            vowel = vk,
            meteg = r.has(M.meteg),
        )
        val prevHasVowel = prev?.let(::hasVowelMark) ?: false
        val prevVk = prev?.let(::vowelKeyOf)
        val prevOut = out.getOrNull(i - 1)

        // ── shin / sin ──
        if (base == 'ש') {
            if (r.has(M.sinDot)) {
                u.shin = "sin"; u.cons = "s"
                u.notes.add("שׂ com o ponto à esquerda (sin): s como em \"sapo\".")
            } else {
                u.shin = "shin"; u.cons = "sh"
                if (r.has(M.shinDot)) u.notes.add("שׁ com o ponto à direita (shin): \"ch\" como em \"chave\".")
                else u.notes.add("ש sem ponto: lê-se shin, \"ch\" como em \"chave\".")
            }
        }

        // ── vav as vowel (mater) ──
        val cholamOnVav = base == 'ו' && r.has(M.cholam) && !r.has(M.cholamVav)
        val otherVowelOnVav = base == 'ו' && r.marks.any { VOWEL_OF[it] != null && VOWEL_OF[it] != "cholam" }
        if (base == 'ו' && !otherVowelOnVav && !shva) {
            val materOk = isFirst || !prevHasVowel
            val chatafBefore = prevVk == "chataf-patach" || prevVk == "chataf-segol" || prevVk == "chataf-kamats"
            if (dag && !cholamOnVav && (materOk || (prev != null && prevVk == null && (prevOut?.vowelSound ?: "").isEmpty()))) {
                u.role = "mater lectionis"; u.cons = ""; u.dagesh = "shuruk"
                u.notes.add("וּ com o ponto no meio é shuruk: a vogal \"u\" (uva); o vav não soa como consoante.")
                rule("Shuruk (וּ): o vav com ponto no meio é a vogal \"u\".")
                lendVowel(u, "shuruk", "u")
                out.add(finish(u))
                continue
            }
            if (cholamOnVav && (materOk || chatafBefore)) {
                u.role = "mater lectionis"; u.cons = ""
                u.notes.add("וֹ com o ponto em cima é cholam male: a vogal \"o\" fechado (avô); o vav não soa como consoante.")
                rule("Cholam male (וֹ): o vav com ponto em cima é a vogal \"o\".")
                lendVowel(u, "cholam", "o")
                out.add(finish(u))
                continue
            }
        }
        if (base == 'ו' && r.has(M.cholamVav)) {
            u.vowel = "cholam"
            u.notes.add("וֺ com cholam haser (ponto próprio do vav consoante): v + o.")
        }
        if (base == 'ו' && dag && vk != null) {
            u.dagesh = "chazak"
            u.notes.add("ו com dagesh e vogal própria é consoante: v como em \"vaca\", dobrado (dagesh chazak).")
        }

        // ── yod as mater / glide ──
        val materVavNext = next != null && next.letter == 'ו' && !next.marks.any { VOWEL_OF[it] != null && VOWEL_OF[it] != "cholam" } && (next.has(M.dagesh) || next.has(M.cholam))
        if (base == 'י' && vk == null && !shva && !isFirst && !dag && !materVavNext) {
            val nextIsFinalVav = next != null && next.letter == 'ו' && next.marks.isEmpty() && i + 2 == raw.size
            if (nextIsFinalVav && (prevVk == "kamats" || prevVk == "patach")) {
                u.role = "consoante muda"; u.cons = ""
                u.notes.add("ָיו no fim da palavra lê-se \"av\": o yod fica mudo e o vav soa v.")
                rule("Terminação ־ָיו: lê-se \"av\" (o yod é mudo).")
                out.add(finish(u))
                continue
            }
            if (prevVk == "chirik") {
                u.role = "mater lectionis"; u.cons = ""
                u.notes.add("י sem vogal depois de chirik: alonga o \"i\" (chirik male); não soa como consoante.")
                rule("Chirik male (ִי): o yod só alonga o \"i\".")
                out.add(finish(u))
                continue
            }
            if (prevVk == "tsere" || prevVk == "segol") {
                u.role = "mater lectionis"; u.cons = ""
                u.notes.add("י sem vogal depois de ${if (prevVk == "tsere") "tsere" else "segol"}: na pronúncia sefardita fecha o \"ê\" (não se ouve um i separado).")
                rule("Tsere/segol + yod (ֵי / ֶי): lê-se \"ê\" fechado; o yod não forma sílaba.")
                out.add(finish(u))
                continue
            }
            if (prevVk == "patach" || prevVk == "kamats" || prevVk == "cholam" || prevVk == "kubuts" || prevOut?.vowel == "shuruk") {
                u.role = "semivogal"; u.cons = ""; u.vowelSound = "i"
                val pv = prevOut?.vowelSound?.ifEmpty { null } ?: SOUND_OF[prevVk] ?: ""
                u.notes.add("י sem vogal depois de \"$pv\": semivogal i — forma o ditongo \"${pv}i\" na mesma sílaba.")
                rule("Ditongo ${pv}i: vogal + yod sem vogal soam juntos na mesma sílaba.")
                out.add(finish(u))
                continue
            }
        }
        if (base == 'י' && (vk != null || shva || isFirst || u.role == "consoante")) {
            u.cons = if (vk == "chirik") "y" else "i"
            if (dag) {
                u.dagesh = "chazak"
                u.notes.add("י com dagesh: consoante dobrada (dagesh chazak).")
            }
            u.notes.add(if (vk == "chirik") "י consoante antes de i: escreve-se \"yi\"." else "י consoante: som de i inicial, como em \"ioiô\".")
        }

        // ── he ──
        if (base == 'ה') {
            if (isLast && dag) {
                u.dagesh = "mappiq"; u.cons = ""
                u.notes.add("הּ com mappiq no fim da palavra: um leve sopro de h, que a transliteração não escreve.")
                rule("Mappiq (הּ): o ponto dentro do ה final manda pronunciar o h.")
            } else if (isLast && vk == null && !shva) {
                u.role = "consoante muda"; u.cons = ""
                u.notes.add("ה final sem ponto é muda: só sustenta a vogal anterior.")
                rule("ה final sem mappiq é muda (mater lectionis).")
            } else if (isLast && !isFirst && vk != null && !dag) {
                u.cons = ""
                u.notes.add("ה final com vogal e sem mappiq: o h não se ouve, só a vogal (sufixo ־ֶיהָ / ־ָה).")
            } else if (vk == null && !shva && !isFirst && !(next != null && next.letter == 'ו' && (next.has(M.dagesh) || next.has(M.cholam) || next.has(M.cholamVav)))) {
                u.role = "consoante muda"; u.cons = ""
                u.notes.add("ה sem vogal no meio da palavra: muda.")
            } else {
                u.notes.add("ה: h aspirado, um sopro leve.")
            }
        }

        // ── alef / ayin ──
        if (base == 'א') {
            if (vk == null && !shva) {
                u.role = "consoante muda"
                u.notes.add(if (isFirst) "א sem vogal no início: muda." else "א sem vogal: muda; não fecha a sílaba (e.g. רֹאשׁ rosh, לֹא lo).")
                if (!isFirst) rule("Alef muda (א sem vogal): não se ouve nem fecha a sílaba.")
            } else {
                u.role = "portadora de vogal"
                u.notes.add("א é muda: só se ouve a vogal que ela carrega.")
            }
        }
        if (base == 'ע') {
            if (vk == null && !shva) {
                u.role = "consoante muda"
                u.notes.add("ע sem vogal: gutural profunda, na prática muda (não se ouve).")
            } else {
                u.role = "portadora de vogal"
                u.notes.add("ע é gutural profunda; na prática só se ouve a vogal que ela carrega.")
            }
        }

        // ── dagesh in the other letters ──
        if (dag && u.dagesh == null && base != 'ו' && base != 'י' && base != 'ה') {
            val prevOpen = prev != null && prevVk != null
            val chazak = !isFirst && prevOpen && !(prev != null && prev.has(M.shva))
            u.dagesh = if (chazak) "chazak" else "kal"
            if (base in BEGADKEFAT && HARD[base] != null) {
                u.cons = HARD.getValue(base)
                u.notes.add("${r.letter} com dagesh: som \"${HARD[base]}\" (sem o ponto seria \"${CONS[base]}\").")
            }
            if (chazak) {
                u.notes.add("Dagesh chazak: a consoante fecha a sílaba anterior e abre a seguinte (dobra-se na pronúncia cuidada).")
                rule("Dagesh chazak (ponto depois de vogal): a consoante é dobrada; um shva sob ela é shva na.")
            } else if (base in BEGADKEFAT) {
                rule("Dagesh kal (ponto em ב כ פ no início ou depois de sílaba fechada): b, k, p em vez de v, ch, f.")
            }
        } else if (!dag && base in BEGADKEFAT && HARD[base] != null && u.role == "consoante") {
            u.notes.add("${r.letter} sem dagesh: som \"${CONS[base]}\"${if (base == 'כ') " (gutural, como ח)" else ""}.")
        }

        // ── vowel sound ──
        if (vk != null && u.vowelSound.isEmpty()) {
            u.vowel = vk
            u.vowelSound = SOUND_OF.getValue(vk)
            if (vk == "kamats") {
                val bareSeg = raw.joinToString("") { (FINAL_TO_BASE[it.letter] ?: it.letter).toString() }
                val kolWord = base == 'כ' && next != null && next.letter == 'ל' && i + 1 == raw.size - 1 && !hasVowelMark(next) && dag && raw.size == 2
                val nextClosesShort = next != null && (next.has(M.shva) || next.has(M.chatafKamats))
                val katanWord = nextClosesShort && KATAN_STEMS.any { st -> bareSeg.indexOf(st) == i }
                if (kolWord || katanWord) {
                    u.vowel = "kamats-katan"
                    u.vowelSound = "o"
                    val beforeChatafKamats = next != null && next.has(M.chatafKamats)
                    u.notes.add(if (beforeChatafKamats) "Kamats katan: antes de chataf kamats o kamats lê-se \"o\"." else "Kamats katan: aqui o kamats lê-se \"o\" (sílaba fechada e átona).")
                    rule(if (beforeChatafKamats) "Kamats katan: antes de chataf kamats, o kamats vale \"o\"." else "Kamats katan: o mesmo sinal do kamats gadol vale \"o\" em sílaba fechada e átona.")
                }
            }
            if (vk == "chataf-patach" || vk == "chataf-segol" || vk == "chataf-kamats") {
                rule("Chataf (vogal composta sob gutural): vogal bem curta, mas sempre pronunciada.")
            }
        }

        // ── shva ──
        if (shva) {
            val prevShva = prev != null && prev.has(M.shva)
            val nextShva = next != null && next.has(M.shva)
            val sameNext = next != null && (FINAL_TO_BASE[next.letter] ?: next.letter) == base
            val restSilent = raw.subList(i + 1, raw.size).all { it.letter == 'א' && it.marks.isEmpty() }
            var prevVowelKey: String? = null
            for (k in i - 1 downTo 0) {
                val o = out[k]
                if (o.vowel != null && o.vowel != "shva-nach") {
                    prevVowelKey = o.vowel
                    break
                }
                if (o.role != "consoante muda" && o.role != "mater lectionis") break
            }
            val effectivelyLast = isLast || restSilent
            val nextIsFinalShva = next != null && next.has(M.shva) && i + 1 == raw.size - 1
            val (na, why) = when {
                effectivelyLast || nextIsFinalShva -> false to "no fim da palavra o shva é nach (mudo)"
                i == 1 && prev != null && prev.letter == 'ו' && prev.has(M.dagesh) && prevVk == null -> false to "depois do וּ inicial (a conjunção \"e\") o shva é nach"
                isFirst -> true to "no início da palavra o shva é na (soa \"e\" curtíssimo)"
                u.dagesh == "chazak" -> true to "sob letra com dagesh chazak o shva é na"
                prevShva -> true to "de dois shvas seguidos, o segundo é na"
                nextShva -> false to "de dois shvas seguidos, o primeiro é nach (fecha a sílaba)"
                sameNext -> true to "entre duas letras iguais o shva é na"
                prevVowelKey != null && prevVowelKey in LONG -> true to "depois de vogal longa (kamats, tsere, cholam, shuruk) o shva é na"
                else -> false to "depois de vogal breve o shva é nach (fecha a sílaba)"
            }
            u.vowel = if (na) "shva-na" else "shva-nach"
            u.vowelSound = if (na) "e" else ""
            u.notes.add("Shva ${if (na) "na" else "nach"}: $why.")
            rule(if (na) "Shva na: soa como um \"e\" curtíssimo." else "Shva nach: mudo, fecha a sílaba.")
        }

        // ── patach ganuv ──
        if (isLast && vk == "patach" && (base == 'ח' || base == 'ע' || (base == 'ה' && dag))) {
            val pOut = out.getOrNull(i - 1)
            val longBefore = pOut != null && ((pOut.vowel ?: "") in listOf("tsere", "cholam", "shuruk", "chirik", "kubuts", "segol") || pOut.role == "mater lectionis")
            if (longBefore) {
                u.ganuv = true
                u.notes.add("Patach ganuv (\"furtivo\"): o \"a\" é pronunciado antes da consoante gutural final (…êach, …úa).")
                rule("Patach ganuv: sob ח ע הּ finais, o patach soa antes da consoante.")
            }
        }

        // ── meteg ──
        if (u.meteg) u.notes.add("Meteg (tracinho): apoio de leitura, marca a sílaba a sustentar; não tem som.")

        out.add(finish(u))
    }
    for (u in out) {
        if (u.base == 'י' && u.role == "consoante" && u.cons.isNotEmpty()) u.cons = if (u.vowelSound == "i") "y" else "i"
        val vowelNext = u.vowelSound
        val fullFront = u.vowel == "tsere" || u.vowel == "segol" || u.vowel == "chirik" || u.vowel == "chataf-segol"
        if (u.cons == "g" || u.cons == "gu") u.cons = if (fullFront) "gu" else "g"
        if (u.cons == "k" || u.cons == "c") u.cons = if (Regex("^[eéêi]").containsMatchIn(vowelNext)) "k" else "c"
        u.tr = if (u.ganuv) "a" + u.cons else if (u.role == "consoante muda" && u.vowelSound.isEmpty()) "" else u.cons + u.vowelSound
    }
    return out
}

private fun normTr(s: String): String = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("[\\u0300-\\u036F]"), "")
    .replace(Regex("[^a-z]"), "")
    .replace("q", "k")
    .replace(Regex("c(?!h)"), "k")
    .replace("y", "i")
    .replace("ss", "s")

/** Align the generated chunks with the engine's letters and let the engine settle kamats a/o and shva e/∅. */
private fun applyGuidance(units: List<PhonUnit>, trRaw: String, rule: (String) -> Unit): Boolean {
    val target = normTr(trRaw)
    val chunks = units.map { normTr(it.tr) }
    val mine = chunks.joinToString("")
    if (target.isEmpty() || mine.isEmpty()) return false
    val a = mine
    val b = target
    val n = a.length
    val m = b.length
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) dp[i][0] = i
    for (j in 0..m) dp[0][j] = j
    for (i in 1..n) for (j in 1..m) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
    }
    if (dp[n][m] > max(2, ceil(max(n, m) * 0.34).toInt())) return false
    val alignedTo = Array(n) { "" }
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + (if (a[i - 1] == b[j - 1]) 0 else 1)) {
            alignedTo[i - 1] = b[j - 1] + alignedTo[i - 1]
            i--; j--
        } else if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
            if (i > 0) alignedTo[i - 1] = b[j - 1] + alignedTo[i - 1]
            else if (n > 0) alignedTo[0] = b[j - 1] + alignedTo[0]
            j--
        } else {
            i--
        }
    }
    var pos = 0
    for (k in units.indices) {
        val u = units[k]
        val len = chunks[k].length
        val got = alignedTo.copyOfRange(pos, min(pos + len, n)).joinToString("")
        val vowelsGot = got.replace(Regex("[^aeiou]"), "")
        if (u.vowel == "kamats" && 'o' in vowelsGot && 'a' !in vowelsGot) {
            u.vowel = "kamats-katan"; u.vowelSound = "o"; u.tr = u.cons + "o"
            u.notes.add("Kamats katan: aqui o kamats lê-se \"o\" (sílaba fechada e átona), como na transliteração.")
            rule("Kamats katan: o mesmo sinal do kamats gadol vale \"o\" em sílaba fechada e átona.")
        } else if (u.vowel == "kamats-katan" && 'a' in vowelsGot && 'o' !in vowelsGot) {
            u.vowel = "kamats"; u.vowelSound = "a"; u.tr = u.cons + "a"
        }
        if (u.vowel == "shva-na" && len > 0 && 'e' !in got && chunks[k].endsWith("e")) {
            u.vowel = "shva-nach"; u.vowelSound = ""; u.tr = u.cons
            u.notes.add("A transliteração não soa este shva: lê-se como shva nach (mudo).")
        }
        pos += len
        if (u.vowel == "shva-nach" && k < units.size - 1) {
            val nextChunk = chunks[k + 1]
            val between = alignedTo.getOrNull(pos - 1) ?: ""
            if (between.length > (chunks[k].takeLast(1)).length && between.endsWith("e") && !nextChunk.startsWith("e")) {
                u.vowel = "shva-na"; u.vowelSound = "e"; u.tr = u.cons + "e"
                u.notes.add("A transliteração soa este shva (\"e\" curtíssimo): shva na.")
            }
        }
    }
    return true
}

private fun syllabify(units: List<PhonUnit>): List<PhonSyllable> {
    val syl = mutableListOf<PhonSyllable>()
    var ci = -1
    fun open() {
        syl.add(PhonSyllable())
        ci = syl.size - 1
    }
    for ((i, u) in units.withIndex()) {
        val hasNucleus = u.vowelSound.isNotEmpty() && u.role != "semivogal"
        if (hasNucleus) {
            if (ci < 0 || syl[ci].nucleusText.isNotEmpty()) open()
            syl[ci].units.add(i)
            syl[ci].nucleusText += u.vowelSound
            if (u.ganuv) syl[ci].open = false
        } else if (u.role == "semivogal") {
            if (ci < 0) open()
            syl[ci].units.add(i)
            syl[ci].nucleusText += u.vowelSound
            syl[ci].nucleus = if (syl[ci].nucleusText.length >= 3) "tritongo" else "ditongo"
        } else if (u.role == "mater lectionis" || u.role == "consoante muda") {
            if (ci < 0) open()
            syl[ci].units.add(i)
        } else {
            if (ci < 0) open()
            syl[ci].units.add(i)
            if (u.cons.isNotEmpty()) syl[ci].open = false
        }
        u.syllable = ci
    }
    return syl
}

private fun pickStress(units: List<PhonUnit>, syl: List<PhonSyllable>, trRaw: String, guided: Boolean): Int {
    if (syl.size <= 1) return 0
    val last = syl.size - 1
    val ganuvLast = syl[last].units.any { units[it].ganuv }
    if (trRaw.isNotEmpty() && guided) {
        val acute = trRaw.indexOfFirst { it in "áéíóú" }
        if (acute >= 0) {
            val vowelsBefore = trRaw.substring(0, acute + 1).replace(Regex("[^aeiouyáéíóúêôâ]", RegexOption.IGNORE_CASE), "").length
            var seen = 0
            for (s in syl.indices) {
                seen += syl[s].units.sumOf { units[it].tr.replace(Regex("[^aeiouyê]"), "").length }
                if (seen >= vowelsBefore) return s
            }
        }
        if (ganuvLast) return last - 1
        val plain = Normalizer.normalize(trRaw, Normalizer.Form.NFC).lowercase().replace(Regex("[^a-zê]"), "")
        if (Regex("ê(nu|cha)$").containsMatchIn(plain)) return last - 1
        val penTsere = syl[last - 1].units.any { units[it].vowel == "tsere" }
        val lastVowel = syl[last].units.map { units[it] }.firstOrNull { it.vowelSound.isNotEmpty() }?.vowel
        if (penTsere && lastVowel == "segol" && !syl[last].open && !Regex("(h|ch)[eê][mn]$").containsMatchIn(plain)) return last - 1
        return last
    }
    val lastU = syl[last].units.map { units[it] }
    val penU = syl[last - 1].units.map { units[it] }
    val lastV = lastU.firstOrNull { it.vowelSound.isNotEmpty() }?.vowel
    val penV = penU.firstOrNull { it.vowelSound.isNotEmpty() }?.vowel
    if (ganuvLast) return last - 1
    if (lastV == "segol" && (penV == "segol" || penV == "tsere" || penV == "cholam" || penV == "patach") && !syl[last].open) return last - 1
    if (lastV == "chirik" && lastU.any { it.base == 'י' && it.cons.isNotEmpty() } && lastU.last().base == 'מ') return last - 1
    return last
}

private fun accentuate(t: String): String {
    val map = mapOf('a' to 'á', 'e' to 'é', 'i' to 'í', 'o' to 'ó', 'u' to 'ú')
    for ((k, c) in t.withIndex()) {
        if (c == 'ê') return t
        map[c]?.let { return t.substring(0, k) + it + t.substring(k + 1) }
    }
    return t
}

private fun findEncounters(units: List<PhonUnit>, syl: List<PhonSyllable>): List<PhonEncounter> {
    val out = mutableListOf<PhonEncounter>()
    syl.forEachIndexed { i, s ->
        if (s.nucleus == "ditongo") out.add(PhonEncounter("ditongo", s.nucleusText, "sílaba ${i + 1} (${s.tr})", "Duas vogais na mesma sílaba: \"${s.nucleusText}\" — ditongo decrescente (vogal + semivogal i)."))
        if (s.nucleus == "tritongo") out.add(PhonEncounter("tritongo", s.nucleusText, "sílaba ${i + 1} (${s.tr})", "Três vogais na mesma sílaba: \"${s.nucleusText}\"."))
    }
    for (i in 1 until syl.size) {
        val a = syl[i - 1]
        val b = syl[i]
        val aLast = a.units.last()
        val bFirstNucleus = b.units.firstOrNull { units[it].vowelSound.isNotEmpty() }
        val bOnsetHasCons = b.units.any { units[it].cons.isNotEmpty() && units[it].tr.startsWith(units[it].cons) && units[it].vowelSound.isNotEmpty() }
        if (a.open && bFirstNucleus != null && !bOnsetHasCons && units[aLast].vowelSound != "") {
            out.add(PhonEncounter("hiato", "${a.nucleusText}-${b.nucleusText}", "${a.tr}·${b.tr}", "Duas vogais seguidas em sílabas diferentes: \"${a.nucleusText}-${b.nucleusText}\" — hiato (a letra entre elas, ${units[bFirstNucleus].letterName}, é muda)."))
        }
        val coda = units[aLast]
        val onset = units[b.units[0]]
        if (!a.open && coda.cons.isNotEmpty() && onset.cons.isNotEmpty() && !onset.ganuv) {
            out.add(PhonEncounter("encontro consonantal", "${coda.cons}-${onset.cons}", "${a.tr}·${b.tr}", "Consoantes em contato entre sílabas: \"${coda.cons}\" fecha a sílaba e \"${onset.cons}\" abre a seguinte — encontro consonantal (disjunto)."))
        }
    }
    return out
}

/** Short pt-BR description of a unit's consonant, from the glossary data. */
fun unitSoundDesc(u: PhonUnit): String {
    val l = Glossary.data.alefBet.firstOrNull { it.key == LETTER_KEY[u.base] } ?: return ""
    if (l.sounds.isEmpty()) return ""
    var s = l.sounds[0]
    if (l.sounds.size == 2) s = if (u.base == 'ש') (if (u.shin == "sin") l.sounds[1] else l.sounds[0]) else if (u.dagesh == "kal" || u.dagesh == "chazak") l.sounds[0] else l.sounds[1]
    return s.sound
}

fun unitVowelDesc(u: PhonUnit): String {
    val v = u.vowel ?: return ""
    return "${VOWEL_NAME[v]}: ${vowelDesc(v).ifEmpty { SOUND_OF[v] ?: "" }}"
}
