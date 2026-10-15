package br.com.shiurhebraico.core.hebrew

/**
 * Hebrew text helpers — ports of the site's utils/hebrew.ts (stripNikud, skeletonTranslit) and
 * utils/marks.ts (the nikud / te'amim switches). Tested against the site's own output.
 */

/** cantillation, points, meteg, rafe, shin/sin dots, upper/lower dots, qamats qatan (maqaf, paseq, sof pasuq kept) */
private val MARKS = Regex("[\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7]")
private val VOWEL = Regex("[\\u05B0-\\u05BB\\u05C7]")
private val ENGINE_CAPITAL = Regex("^[A-ZÁÉÍÓÚÂÊÔÃÕ]")

fun stripNikud(s: String): String = s.replace(MARKS, "")

private fun isMark(c: Int) = c in 0x0591..0x05BD || c == 0x05BF || c == 0x05C1 || c == 0x05C2 || c == 0x05C4 || c == 0x05C5 || c == 0x05C7

/** Letters-only transliteration for reading practice (the vowel points left to the reader). */
fun skeletonTranslit(he: String, tr: String = ""): String {
    val bare = stripNikud(he)
    if (bare.contains("יהוה")) return tr.ifEmpty { bare }
    val pointed = MARKS.containsMatchIn(he)
    data class Unit(val ch: Int, val marks: StringBuilder)
    val units = mutableListOf<Unit>()
    he.codePoints().forEach { c ->
        if (isMark(c)) {
            units.lastOrNull()?.marks?.appendCodePoint(c)
        } else units.add(Unit(c, StringBuilder()))
    }
    val out = StringBuilder()
    for ((cp, sb) in units) {
        val marks = sb.toString()
        val dagesh = marks.contains('ּ')
        val ownVowel = VOWEL.containsMatchIn(marks)
        when (cp.toChar()) {
            'א', 'ע' -> out.append('\'')
            'ב' -> out.append(if (dagesh || !pointed) "b" else "v")
            'ג' -> out.append('g')
            'ד' -> out.append('d')
            'ה' -> out.append('h')
            'ו' -> out.append(if (marks.contains('ֹ') || marks.contains('ֺ')) "o" else if (dagesh && !ownVowel) "u" else "v")
            'ז' -> out.append('z')
            'ח' -> out.append("ch")
            'ט' -> out.append('t')
            'י' -> out.append(if (pointed && !ownVowel && !dagesh) "i" else "y")
            'כ', 'ך' -> out.append(if (dagesh || !pointed) "k" else "ch")
            'ל' -> out.append('l')
            'מ', 'ם' -> out.append('m')
            'נ', 'ן' -> out.append('n')
            'ס' -> out.append('s')
            'פ', 'ף' -> out.append(if (dagesh || !pointed) "p" else "f")
            'צ', 'ץ' -> out.append("tz")
            'ק' -> out.append('k')
            'ר' -> out.append('r')
            'ש' -> out.append(if (marks.contains('ׂ')) "s" else "sh")
            'ת' -> out.append('t')
            '־' -> out.append('-')
            else -> out.appendCodePoint(cp)
        }
    }
    var s = out.toString()
    // follow the engine's capitalisation (proper names, sentence starts)
    if (tr.isNotEmpty() && ENGINE_CAPITAL.containsMatchIn(tr)) {
        val i = s.indexOfFirst { it in 'a'..'z' }
        if (i >= 0) s = s.substring(0, i) + s[i].uppercaseChar() + s.substring(i + 1)
    }
    return s
}

/** Which marks the reader shows: the vowel points and the cantillation accents, each on its own. */
data class Marks(val nikud: Boolean = true, val teamim: Boolean = true)

private val POINTS = Regex("[\\u05B0-\\u05BC\\u05C1\\u05C2\\u05C7]")
private val TEAMIM = Regex("[\\u0591-\\u05AF\\u05BD\\u05BF]")

fun applyMarks(text: String, m: Marks): String {
    var out = text
    if (!m.teamim) out = out.replace(TEAMIM, "")
    if (!m.nikud) out = out.replace(POINTS, "")
    return out
}
