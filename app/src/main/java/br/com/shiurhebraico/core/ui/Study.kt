package br.com.shiurhebraico.core.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.shiurhebraico.core.hebrew.Glossary
import br.com.shiurhebraico.core.hebrew.Marks
import br.com.shiurhebraico.core.hebrew.analyzeWord
import br.com.shiurhebraico.core.hebrew.unitSoundDesc
import br.com.shiurhebraico.core.hebrew.unitVowelDesc
import br.com.shiurhebraico.core.theme.HebrewFamily
import br.com.shiurhebraico.core.theme.HebrewTextFamily
import br.com.shiurhebraico.core.theme.MonoLabel
import java.util.Locale

/** the site's small mono chip (estudo-mast__chip): on = filled ink */
@Composable
fun Chip(label: String, on: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Text(
        label, style = MonoLabel, color = if (on) c.onPrimary else if (enabled) c.onSurfaceVariant else c.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) c.primary else Color.Transparent)
            .border(1.dp, c.outline, RoundedCornerShape(999.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Nikud / te'amim, each switched on its own (the site's MarksMenu), shared by the Siddur and the Parashá. */
@Composable
fun MarksMenu(marks: Marks, onChange: (Marks) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip("SINAIS", !marks.nikud || !marks.teamim) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("Nikud (vogais)", Modifier.width(150.dp)); Switch(marks.nikud, { onChange(marks.copy(nikud = it)) }) } },
                onClick = { onChange(marks.copy(nikud = !marks.nikud)) },
            )
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("Te'amim (cantilação)", Modifier.width(150.dp)); Switch(marks.teamim, { onChange(marks.copy(teamim = it)) }) } },
                onClick = { onChange(marks.copy(teamim = !marks.teamim)) },
            )
        }
    }
}

/** The word's phonetics (the site's PhonPopover): syllables, stress, letter by letter, the rules that fired. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhonSheet(he: String, tr: String, pt: String, onClose: () -> Unit) {
    val a = remember(he, tr) { analyzeWord(he, tr) }
    val ctx = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(ctx) { ok -> if (ok == TextToSpeech.SUCCESS) { engine?.language = Locale("he", "IL"); tts = engine } }
        onDispose { engine?.shutdown() }
    }
    val c = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onClose) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            item {
                Text(a.he, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 40.sp, textDirection = TextDirection.Rtl), color = c.onSurface, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(a.trSyllables, style = MaterialTheme.typography.titleLarge, color = c.onSurface, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (pt.isNotBlank() && !pt.startsWith("[")) Text(pt, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("${a.count} · ${a.tonicity}".uppercase(), style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                if (a.divineName) Text("Nome divino — lê-se Adonai.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (tts != null && !a.divineName) {
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "♪ OUVIR", style = MonoLabel, color = c.onPrimary,
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.primary)
                                .clickable { tts?.speak(he, TextToSpeech.QUEUE_FLUSH, null, "phon") }.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("SÍLABAS", style = MonoLabel, color = c.onSurfaceVariant)
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    a.syllables.forEach { s ->
                        Column(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(if (s.stressed) c.primary else c.surfaceVariant).padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(s.he, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 20.sp, textDirection = TextDirection.Rtl), color = if (s.stressed) c.onPrimary else c.onSurface)
                            Text(s.tr, style = MaterialTheme.typography.bodySmall, color = if (s.stressed) c.onPrimary else c.onSurfaceVariant)
                        }
                    }
                }
                Text("LETRA POR LETRA", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            items(a.units) { u ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(u.glyph, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 26.sp, textDirection = TextDirection.Rtl), color = c.onSurface, modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${u.letterName} · ${u.role}" + (if (u.tr.isNotEmpty()) " · “${u.tr}”" else ""),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = c.onSurface,
                        )
                        unitSoundDesc(u).takeIf { it.isNotEmpty() && u.cons.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                        unitVowelDesc(u).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                        u.notes.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                    }
                }
            }
            item {
                if (a.encounters.isNotEmpty()) {
                    Text("ENCONTROS", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    a.encounters.forEach { e -> Text("${e.kind} · ${e.where} — ${e.desc}", style = MaterialTheme.typography.bodySmall, color = c.onSurface, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                if (a.rules.isNotEmpty()) {
                    Text("REGRAS DE LEITURA", style = MonoLabel, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    a.rules.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall, color = c.onSurface, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                if (a.guided) Text("Sílabas e acento conferidos com a transliteração do Sidur.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** The Glossário fonético: the Alef-Bet and the Nikud in Portuguese (the site's /glossario/ charts). */
@Composable
fun GlossarySheet(onClose: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val c = MaterialTheme.colorScheme
    val g = Glossary.data
    ModalBottomSheet(onDismissRequest = onClose) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("ALEF-BET", tab == 0) { tab = 0 }
            Chip("NIKUD", tab == 1) { tab = 1 }
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            if (tab == 0) {
                items(g.alefBet) { l ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(l.glyph, style = TextStyle(fontFamily = HebrewFamily, fontSize = 40.sp), color = c.onSurface, modifier = Modifier.width(56.dp))
                            Column(Modifier.weight(1f)) {
                                Text(l.name, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                                Text("${l.nameHe} · valor ${l.value}" + (l.final?.let { " · final ${it.glyph}" } ?: ""), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            }
                        }
                        l.sounds.forEach { s ->
                            Text("${s.glyph}  ${s.sound}", style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.padding(top = 4.dp))
                            Text("${s.example.he} · ${s.example.tr} · ${s.example.pt}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        }
                        l.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                        Text(l.mnemonic.hook, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = c.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                items(g.nikud) { n ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(n.glyph, style = TextStyle(fontFamily = HebrewTextFamily, fontSize = 34.sp), color = c.onSurface, modifier = Modifier.width(56.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${n.name}" + (if (n.tr.isNotEmpty()) " · ${n.tr}" else ""), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                            Text(n.sound, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                            if (n.write.isNotEmpty()) Text(n.write, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            n.example?.let { Text("${it.he} · ${it.tr} · ${it.pt}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
