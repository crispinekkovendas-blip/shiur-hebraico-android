package br.com.shiurhebraico.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.shiurhebraico.core.theme.MonoLabel
import br.com.shiurhebraico.parasha.Resolver

/**
 * The site's light HTML (p, h3, blockquote, b, em, br, lists) and its tagged commentary (the nine
 * tags of tag-spec.md: ref, midrash-ref, sage, figure, work, divine, lemma, gloss, he) as Compose text.
 * As on the site since 2026-09-16, the Hebrew <he> lemmas are dropped: the translation stands alone.
 */
data class TBlock(val kind: String, val text: AnnotatedString)

private val TAG = Regex("<(/?)([a-zA-Z][a-zA-Z0-9-]*)([^>]*)>")
private val ATTR = Regex("(\\w+)=\"([^\"]*)\"")
private val BLOCKS = setOf("p", "h3", "h4", "blockquote", "li", "ul", "ol", "div")
private val INTERACTIVE = setOf("ref", "midrash-ref", "sage", "figure", "work", "divine")

private fun entities(s: String) = s.replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

/** drop the Hebrew source spans (and the period after them, and a <b> left empty) */
fun stripSource(html: String): String = html
    .replace(Regex("<he\\b[^>]*>[\\s\\S]*?</he>\\s*[.:]?\\s*"), "")
    .replace(Regex("<b>\\s*</b>\\s*"), "")
    .replace(Regex("[ \\t]{2,}"), " ")
    .trim()

fun parseTagged(raw: String, accent: Color): List<TBlock> {
    val html = stripSource(raw)
    val out = mutableListOf<TBlock>()
    var b = AnnotatedString.Builder()
    var kind = "p"
    var bold = 0
    var italic = 0
    val open = ArrayDeque<String>() // interactive tag annotations in force
    fun flush() {
        val t = b.toAnnotatedString()
        if (t.text.isNotBlank()) out.add(TBlock(kind, t))
        b = AnnotatedString.Builder()
    }
    fun text(s: String) {
        if (s.isEmpty()) return
        val start = b.length
        b.append(entities(s).replace(Regex("\\s+"), " "))
        val end = b.length
        if (bold > 0) b.addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
        if (italic > 0) b.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
        if (open.isNotEmpty()) {
            b.addStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline), start, end)
            b.addStringAnnotation("tag", open.last(), start, end)
        }
    }
    var pos = 0
    for (m in TAG.findAll(html)) {
        text(html.substring(pos, m.range.first))
        pos = m.range.last + 1
        val closing = m.groupValues[1] == "/"
        val name = m.groupValues[2].lowercase()
        when {
            name == "br" -> b.append("\n")
            name in BLOCKS -> {
                flush()
                kind = if (closing) "p" else when (name) { "h3", "h4" -> "h"; "blockquote" -> "quote"; "li" -> "li"; else -> "p" }
            }
            name == "b" || name == "strong" || name == "lemma" -> bold += if (closing) -1 else 1
            name == "em" || name == "i" || name == "gloss" -> italic += if (closing) -1 else 1
            name in INTERACTIVE -> if (closing) open.removeLastOrNull() else {
                val attrs = ATTR.findAll(m.groupValues[3]).associate { it.groupValues[1] to it.groupValues[2] }
                open.addLast("$name|${attrs["key"] ?: ""}|${attrs["cite"] ?: ""}")
            }
        }
        bold = bold.coerceAtLeast(0)
        italic = italic.coerceAtLeast(0)
    }
    text(html.substring(pos))
    flush()
    return out
}

/** Rich text from the site's HTML; tapping a tagged name shows who or what it is. */
@Composable
fun TaggedText(html: String, resolvers: Map<String, Resolver> = emptyMap(), style: TextStyle = MaterialTheme.typography.bodyMedium, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val accent = c.primary.copy(alpha = 0.85f)
    val blocks = remember(html) { parseTagged(html, accent) }
    var tap by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        blocks.forEach { bl ->
            val st = when (bl.kind) {
                "h" -> MaterialTheme.typography.titleMedium
                "quote" -> style.copy(fontStyle = FontStyle.Italic)
                else -> style
            }.copy(color = if (bl.kind == "quote") c.onSurfaceVariant else c.onSurface)
            ClickableText(
                if (bl.kind == "li") AnnotatedString("• ") + bl.text else bl.text,
                style = st,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = if (bl.kind == "quote") 12.dp else 0.dp),
            ) { off -> bl.text.getStringAnnotations("tag", off, off).firstOrNull()?.let { tap = it.item } }
        }
    }
    tap?.let { t ->
        val (name, key, cite) = (t.split("|") + listOf("", "", "")).take(3)
        val r = resolvers[key]
        ModalBottomSheet(onDismissRequest = { tap = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(
                    when (name) { "sage" -> "SÁBIO"; "figure" -> "PERSONAGEM"; "work" -> "OBRA"; "divine" -> "NOME DIVINO"; else -> "FONTE" },
                    style = MonoLabel, color = c.onSurfaceVariant,
                )
                Text(r?.displayPt ?: cite.ifEmpty { key }, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                if (!r?.displayHe.isNullOrEmpty()) Text(r!!.displayHe, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                if (!r?.bio.isNullOrEmpty()) Text(r!!.bio, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp))
                if ((r?.names?.size ?: 0) > 1) Text("Também: " + r!!.names.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                if (cite.isNotEmpty() && r != null) Text(cite, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}
