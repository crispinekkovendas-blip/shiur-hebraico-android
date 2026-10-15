package br.com.shiurhebraico

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

private val RATES = listOf(
    1f to PlayerConstants.PlaybackRate.RATE_1,
    1.25f to PlayerConstants.PlaybackRate.RATE_1_25,
    1.5f to PlayerConstants.PlaybackRate.RATE_1_5,
    1.75f to PlayerConstants.PlaybackRate.RATE_1_75,
    2f to PlayerConstants.PlaybackRate.RATE_2,
    0.75f to PlayerConstants.PlaybackRate.RATE_0_75,
)

private val TABS = listOf("Resumo", "Momentos", "Perguntas", "Fontes", "Transcrição")

/** the playing clock: only the small pieces that show it read it, so a tick never redraws the page */
@Stable
class PlayClock(t0: Double, d0: Double) {
    var t by mutableDoubleStateOf(t0)
    var dur by mutableDoubleStateOf(d0)
}

private fun View.findWebView(): WebView? = when (this) {
    is WebView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findWebView() }
    else -> null
}

/** YouTube's own captions off, whatever the account or the video asks (the IFrame API, in the player page) */
private const val NO_CAPTIONS = "try{player.unloadModule('captions');player.unloadModule('cc');player.setOption('captions','track',{});}catch(e){}"

@Composable
fun AulaScreen(c: Catalog, id: String, repo: Repo, progress: Progress, nav: Nav) {
    val p = LocalPalette.current
    val l = c.lessonById[id] ?: run { Centered("Aula não encontrada."); return }
    val rav = c.ravBySlug[l.r]
    val ctx = LocalContext.current
    val view = LocalView.current
    val activity = ctx as? Activity
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var manualFull by remember { mutableStateOf(false) }
    val full = manualFull || landscape

    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val clock = remember { PlayClock(progress.position(id).toDouble(), l.d.toDouble()) }
    var playing by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var rate by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var failed by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(progress.lang) }
    // the dub: the phone's voice reads the translation, the rabbi turned down under it
    var dub by remember { mutableStateOf(progress.dub) }
    var dubNote by remember { mutableStateOf<String?>(null) }
    val dubber = remember { Dubber(ctx) }
    DisposableEffect(Unit) { onDispose { dubber.shutdown() } }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    var subs by remember { mutableStateOf<Subs?>(null) }
    var study by remember { mutableStateOf<Study?>(null) }
    var loadError by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        runCatching { repo.subs(id) }.onSuccess { subs = it }.onFailure { loadError = true }
    }
    LaunchedEffect(id) { runCatching { repo.study(id) }.onSuccess { study = it } }

    // the course order: the lesson after this one in the teacher's modules
    val next = remember(c, id) {
        val flat = c.modulesOf(c.byRav[l.r].orEmpty()).flatMap { it.second }
        flat.getOrNull(flat.indexOfFirst { it.id == id } + 1)
    }

    fun seek(s: Double) {
        val pl = player ?: return
        pl.seekTo(s.coerceAtLeast(0.0).toFloat())
        dubber.stop()
        clock.t = s
        if (!playing) pl.play()
        started = true
        ended = false
    }
    fun toggle() {
        val pl = player ?: return
        if (playing) pl.pause() else { pl.play(); started = true }
    }

    // Android 13+: ask once for the notification that carries the controls when the screen is off
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(playing) {
        if (playing && Build.VERSION.SDK_INT >= 33 && !progress.askedNotify &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            progress.askedNotify = true
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // the screen stays on while the lesson plays
    DisposableEffect(playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }

    // the media notification and lock-screen controls: listening goes on with the screen off
    DisposableEffect(player) {
        PlayerBridge.onPlay = { player?.play() }
        PlayerBridge.onPause = { player?.pause() }
        PlayerBridge.onBack10 = { seek(clock.t - 10) }
        PlayerBridge.onFwd10 = { seek(clock.t + 10) }
        onDispose {}
    }
    LaunchedEffect(playing, started) {
        if (started) PlaybackService.update(ctx, l.title, rav?.name ?: "Shiur Hebraico", playing, clock.t)
    }
    DisposableEffect(Unit) {
        onDispose {
            PlayerBridge.onPlay = null
            PlayerBridge.onPause = null
            PlayerBridge.onBack10 = null
            PlayerBridge.onFwd10 = null
            PlaybackService.stop(ctx)
        }
    }

    // full screen: landscape, no system bars; back leaves it
    DisposableEffect(full) {
        val w = activity?.window
        if (w != null) {
            val ctl = WindowCompat.getInsetsController(w, w.decorView)
            if (full) {
                ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                ctl.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                ctl.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }
    DisposableEffect(manualFull) {
        activity?.requestedOrientation = if (manualFull) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {}
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
            if (clock.t > 5) progress.save(id, clock.t.toFloat(), clock.dur.toFloat())
        }
    }
    BackHandler(enabled = manualFull) { manualFull = false }

    // where it was left, every few seconds while it plays
    LaunchedEffect(playing) {
        while (playing) {
            delay(5000)
            if (clock.t > 5) progress.save(id, clock.t.toFloat(), clock.dur.toFloat())
        }
    }

    // the line under the voice changes every few seconds, not every tick
    val cur by remember { derivedStateOf { subs?.let { cueAt(it.cues, clock.t) } ?: -1 } }
    val momentNow by remember { derivedStateOf { study?.moments?.indexOfLast { it.t <= clock.t } ?: -1 } }
    val dubbing = dub && lang != "he"
    LaunchedEffect(dubbing, lang, player) {
        player?.setVolume(if (dubbing) 15 else 100)
        if (!dubbing) dubber.stop()
        dubNote = if (dubbing && !dubber.setLang(lang)) "Este celular não tem a voz ${if (lang == "en") "em inglês" else "em português"}: instale em Configurações → Texto para fala." else null
    }
    LaunchedEffect(playing) { if (!playing) dubber.stop() }

    val line = subs?.let { s ->
        if (cur < 0) "" else when (lang) {
            "he" -> s.cues[cur].he
            "en" -> s.en?.getOrNull(cur) ?: s.pt?.getOrNull(cur) ?: ""
            else -> s.pt?.getOrNull(cur) ?: s.en?.getOrNull(cur) ?: ""
        }
    } ?: ""
    LaunchedEffect(cur) {
        val s = subs
        if (dubbing && playing && s != null && cur >= 0) {
            val nextStart = s.cues.getOrNull(cur + 1)?.start ?: s.cues[cur].end
            dubber.say(line, nextStart - s.cues[cur].start)
        }
    }

    val onLang: (String) -> Unit = { lang = it; progress.lang = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val onDub: () -> Unit = { dub = !dub; progress.dub = dub; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val onRate: () -> Unit = { rate = (rate + 1) % RATES.size; player?.setPlaybackRate(RATES[rate].second) }

    Column(Modifier.fillMaxSize().background(if (full) Color.Black else p.paper).then(if (full) Modifier else Modifier.statusBarsPadding())) {
        // ── the video ── (always the first child, so the player survives the switch to full screen)
        BoxWithConstraints(if (full) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            // YouTube draws its title strip and logo at the top and bottom of its frame: the frame is
            // the video's own 16:9 plus 120 dp, so those land in the band we clip away
            val frameW = minOf(maxWidth, maxHeight * 16f / 9f)
            val frameH = frameW * 9f / 16f
            Box(Modifier.fillMaxSize().background(Color.Black).clipToBounds(), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = Modifier.requiredSize(frameW, frameH + 120.dp),
                    factory = { vctx ->
                        YouTubePlayerView(vctx).apply {
                            enableAutomaticInitialization = false
                            // keeps playing with the screen off (the media notification holds the app)
                            enableBackgroundPlayback(true)
                            val yt = this
                            val options = IFramePlayerOptions.Builder(vctx).controls(0).rel(0).ivLoadPolicy(3).ccLoadPolicy(0).build()
                            initialize(
                                object : AbstractYouTubePlayerListener() {
                                    override fun onReady(youTubePlayer: YouTubePlayer) {
                                        player = youTubePlayer
                                        youTubePlayer.cueVideo(id, progress.position(id).let { if (it > 5) it - 2 else 0f })
                                        yt.findWebView()?.evaluateJavascript(NO_CAPTIONS, null)
                                    }

                                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                        if (dragging == null) clock.t = second.toDouble()
                                    }

                                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                        if (duration > 0) clock.dur = duration.toDouble()
                                    }

                                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                        playing = state == PlayerConstants.PlayerState.PLAYING
                                        if (playing) {
                                            started = true
                                            ended = false
                                            yt.findWebView()?.evaluateJavascript(NO_CAPTIONS, null)
                                        }
                                        if (state == PlayerConstants.PlayerState.ENDED) { ended = true; progress.save(id, 0f, clock.dur.toFloat()) }
                                        if (state == PlayerConstants.PlayerState.PAUSED && clock.t > 5) progress.save(id, clock.t.toFloat(), clock.dur.toFloat())
                                    }

                                    override fun onApiChange(youTubePlayer: YouTubePlayer) {
                                        yt.findWebView()?.evaluateJavascript(NO_CAPTIONS, null)
                                    }

                                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                        failed = true
                                    }
                                },
                                true,
                                options,
                            )
                        }
                    },
                    onRelease = { it.release() },
                )
                // the web view keeps every touch that lands on it: this layer takes them (play / pause)
                Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { toggle() })
                Fade(!started) {
                    Box(Modifier.fillMaxSize().clickable { player?.play(); started = true }, contentAlignment = Alignment.Center) {
                        AsyncImage(model = l.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Color(0x33000000)))
                        Box(Modifier.size(68.dp).clip(CircleShape).background(p.tekhelet), contentAlignment = Alignment.Center) {
                            if (player == null) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            else Icon(Icons.Filled.PlayArrow, "Assistir", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        if (progress.position(id) > 5) {
                            Text("Continuar de ${clock(progress.position(id).toDouble())}", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                Fade(ended) {
                    Column(Modifier.fillMaxSize().background(Color(0xE6101828)).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        if (next != null) {
                            Text("PRÓXIMA AULA", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3CBF3))
                            Text(next.title, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill("Assistir de novo", Icons.Filled.Replay) { seek(0.0) }
                            if (next != null) Pill("Próxima aula", Icons.Filled.SkipNext, strong = true) { nav.lesson(next.id) }
                        }
                    }
                }
                if (failed) {
                    Text("O YouTube não deixou tocar este vídeo aqui.", color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp), textAlign = TextAlign.Center)
                }
                if (!full) {
                    IconButton(onClick = nav.back, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(40.dp).clip(CircleShape).background(Color(0x66000000))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                }
            }
            if (full) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9080C16)))).padding(top = 28.dp)) {
                    Band(line, lang, clock, playing, rate, dragging, true, onLang, dub, onDub, onRate,
                        onDrag = { dragging = it }, onSeek = { f -> dragging = null; seek(f * clock.dur) }, onBack10 = { seek(clock.t - 10) }, onFwd10 = { seek(clock.t + 10) },
                        onPlay = { toggle() }, full = true, onFull = { manualFull = false; if (landscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT })
                }
            }
        }
        if (full) return@Column

        // ── the subtitle band, under the video ──
        Band(line, lang, clock, playing, rate, dragging, subs != null, onLang, dub, onDub, onRate,
            onDrag = { dragging = it }, onSeek = { f -> dragging = null; seek(f * clock.dur) }, onBack10 = { seek(clock.t - 10) }, onFwd10 = { seek(clock.t + 10) },
            onPlay = { toggle() }, full = false, onFull = { manualFull = true })

        AnimatedVisibility(dubNote != null) {
            Text(dubNote ?: "", style = MaterialTheme.typography.bodySmall, color = p.novo, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }

        // ── the study ──
        ChipRow(TABS.mapIndexed { i, s -> s to (i == tab) }, { tab = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.fillMaxWidth().background(p.paper).padding(vertical = 8.dp))
        HorizontalDivider(color = p.line)
        Crossfade(tab, Modifier.weight(1f).fillMaxWidth(), animationSpec = tween(180), label = "tab") { k ->
            Box(Modifier.fillMaxSize()) {
                when (k) {
                    0 -> Resumo(c, l, rav, study, next, nav)
                    1 -> Momentos(study, momentNow, ::seek)
                    2 -> Perguntas(study, ::seek)
                    3 -> Fontes(study, ::seek)
                    else -> Transcricao(subs, cur, lang, loadError, ::seek)
                }
            }
        }
    }
}

/** an overlay that fades in and out (outside any Column/Row scope) */
@Composable
private fun Fade(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(260))) { content() }
}

@Composable
private fun Pill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, strong: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (strong) Color.White else Color(0x33FFFFFF)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (strong) Color(0xFF16213A) else Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = if (strong) Color(0xFF16213A) else Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
    }
}

/** the band: the subtitle line, large, then our controls */
@Composable
private fun Band(
    line: String,
    lang: String,
    clock: PlayClock,
    playing: Boolean,
    rate: Int,
    dragging: Float?,
    hasSubs: Boolean,
    onLang: (String) -> Unit,
    dub: Boolean,
    onDub: () -> Unit,
    onRate: () -> Unit,
    onDrag: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    onBack10: () -> Unit,
    onFwd10: () -> Unit,
    onPlay: () -> Unit,
    full: Boolean,
    onFull: () -> Unit,
) {
    val bg = if (full) Color.Transparent else Color(0xFF0D1626)
    Column(Modifier.fillMaxWidth().background(bg).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = if (full) 44.dp else 62.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(line, transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) }, label = "line") { text ->
                Text(
                    text,
                    color = Color.White,
                    fontFamily = if (lang == "he") Serif else Sans,
                    fontSize = if (lang == "he") 20.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = if (lang == "he") TextDirection.Rtl else TextDirection.Ltr),
                )
            }
            if (!hasSubs) CircularProgressIndicator(color = Color(0x88FFFFFF), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CtlIcon(Icons.Filled.Replay10, "Voltar 10 segundos", onBack10)
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
                Crossfade(playing, animationSpec = tween(150), label = "pp") { on ->
                    Icon(if (on) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (on) "Pausar" else "Assistir", tint = Color(0xFF0D1626), modifier = Modifier.size(28.dp))
                }
            }
            CtlIcon(Icons.Filled.Forward10, "Avançar 10 segundos", onFwd10)
            TimeBar(clock, dragging, onDrag, onSeek, Modifier.weight(1f))
            Text(
                "${RATES[rate].first.toString().removeSuffix(".0")}×",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onRate).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
            )
            CtlIcon(if (full) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Tela cheia", onFull)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val dubBg by animateColorAsState(if (dub) Color(0xFF86AAEA) else Color.Transparent, tween(200), label = "dub")
            Row(
                Modifier.padding(end = 6.dp).clip(RoundedCornerShape(50)).background(dubBg)
                    .border(1.dp, if (dub) Color(0xFF86AAEA) else Color(0x55FFFFFF), RoundedCornerShape(50)).clickable(onClick = onDub).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.RecordVoiceOver, "Dublagem", tint = if (dub) Color(0xFF0D1626) else Color.White, modifier = Modifier.size(15.dp))
                Text("Dublagem", color = if (dub) Color(0xFF0D1626) else Color(0xCCFFFFFF), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
            }
            listOf("pt" to "PT", "en" to "EN", "he" to "עב").forEach { (k, label) ->
                val on = lang == k
                val chipBg by animateColorAsState(if (on) Color.White else Color.Transparent, tween(200), label = "lang$k")
                Text(
                    label,
                    color = if (on) Color(0xFF0D1626) else Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(50)).background(chipBg).clickable { onLang(k) }.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** the seek bar and the time: the only part of the band that follows every tick */
@Composable
private fun TimeBar(clock: PlayClock, dragging: Float?, onDrag: (Float) -> Unit, onSeek: (Float) -> Unit, modifier: Modifier) {
    val frac = dragging ?: (if (clock.dur > 0) (clock.t / clock.dur).toFloat() else 0f)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = frac.coerceIn(0f, 1f),
            onValueChange = onDrag,
            onValueChangeFinished = { onSeek(dragging ?: frac) },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF86AAEA), inactiveTrackColor = Color(0x44FFFFFF)),
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
        )
        Text(clock(if (dragging != null) dragging * clock.dur else clock.t), color = Color(0xCCFFFFFF), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CtlIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/* ── tabs ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun Resumo(c: Catalog, l: Lesson, rav: Rav?, study: Study?, next: Lesson?, nav: Nav) {
    val p = LocalPalette.current
    val more = remember(c, l.id) { c.byRav[l.r].orEmpty().filter { it.id != l.id && it.id != next?.id }.sortedByDescending { it.p }.take(6) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(c.placeOf(l))
                    YearTag(l.y)
                    Text(duration(l.d), style = MaterialTheme.typography.labelSmall, color = p.muted)
                }
                Text(l.title, style = MaterialTheme.typography.headlineSmall, color = p.ink, modifier = Modifier.padding(top = 8.dp))
                if (l.t.isNotBlank()) HebrewText(l.t, 16, p.muted, Modifier.fillMaxWidth().padding(top = 6.dp), weight = FontWeight.Normal)
                if (rav != null) {
                    Row(
                        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface).border(1.dp, p.line, RoundedCornerShape(12.dp)).clickable { nav.rav(rav.slug) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Portrait(rav, 44.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(rav.name, style = MaterialTheme.typography.titleSmall, color = p.ink)
                            Text("${c.byRav[rav.slug]?.size ?: 0} aulas · ver o curso", style = MaterialTheme.typography.bodySmall, color = p.tekhelet)
                        }
                        c.tradName(rav).takeIf { it.isNotBlank() }?.let { Tag(it) }
                    }
                }
                val summary = study?.summary?.ifBlank { null } ?: l.sum
                if (summary.isNotBlank()) {
                    Text("Resumo", style = MaterialTheme.typography.titleLarge, color = p.ink, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                    summary.split("\n").filter { it.isNotBlank() }.forEach {
                        Text(it.trim(), style = MaterialTheme.typography.bodyLarge, color = p.ink, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                if (study != null) {
                    Text(
                        "Neste estudo: ${study.moments.size} momentos, ${study.questions.size} perguntas e ${study.sources.size} fontes citadas pelo mestre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.muted,
                    )
                }
            }
        }
        if (next != null) {
            item { SectionTitle("Próxima aula do curso") }
            item { LessonRow(c, next, showRav = false, onOpen = nav.lesson) }
        }
        if (more.isNotEmpty()) {
            item { SectionTitle("Mais deste mestre") }
            items(more, key = { it.id }) { x -> LessonRow(c, x, showRav = false, onOpen = nav.lesson) }
        }
    }
}

@Composable
private fun Loading(study: Study?, content: @Composable (Study) -> Unit) {
    if (study == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else content(study)
}

@Composable
private fun Momentos(study: Study?, now: Int, seek: (Double) -> Unit) = Loading(study) { s ->
    val p = LocalPalette.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(s.moments) { i, m ->
            Row(
                Modifier.fillMaxWidth().background(if (i == now) p.wash else Color.Transparent).clickable { seek(m.t) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(clock(m.t), style = MaterialTheme.typography.labelMedium, color = p.tekhelet, modifier = Modifier.width(64.dp).padding(top = 2.dp))
                Text(m.label, style = MaterialTheme.typography.bodyLarge, color = p.ink, fontWeight = if (i == now) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun Perguntas(study: Study?, seek: (Double) -> Unit) = Loading(study) { s ->
    val p = LocalPalette.current
    val open = remember { mutableStateOf(setOf<Int>()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Responda de cabeça, depois confira com o que o mestre disse.", style = MaterialTheme.typography.bodySmall, color = p.muted) }
        itemsIndexed(s.questions) { i, q ->
            val isOpen = i in open.value
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface).border(1.dp, p.line, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text("${i + 1}. ${q.q}", style = MaterialTheme.typography.titleMedium, color = p.ink)
                if (isOpen) Text(q.a, style = MaterialTheme.typography.bodyLarge, color = p.ink, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (isOpen) "Esconder a resposta" else "Ver a resposta", style = MaterialTheme.typography.labelLarge, color = p.tekhelet, modifier = Modifier.clickable { open.value = if (isOpen) open.value - i else open.value + i }.padding(vertical = 4.dp))
                    Text("Ir ao momento ${clock(q.t)}", style = MaterialTheme.typography.labelLarge, color = p.muted, modifier = Modifier.clickable { seek(q.t) }.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun Fontes(study: Study?, seek: (Double) -> Unit) = Loading(study) { s ->
    val p = LocalPalette.current
    val groups = remember(s) { SOURCE_LABELS.keys.map { k -> k to s.sources.filter { (if (it.kind in SOURCE_LABELS) it.kind else "outro") == k } }.filter { it.second.isNotEmpty() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (groups.isEmpty()) item { Text("Nenhuma fonte registrada nesta aula.", color = p.muted, modifier = Modifier.padding(16.dp)) }
        groups.forEach { (k, xs) ->
            item(key = "h$k") { SectionTitle("${SOURCE_LABELS[k]} · ${xs.size}") }
            items(xs) { x ->
                Column(Modifier.fillMaxWidth().clickable { seek(x.t) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(x.ref, style = MaterialTheme.typography.titleSmall, color = p.ink, modifier = Modifier.weight(1f))
                        Text(clock(x.t), style = MaterialTheme.typography.labelMedium, color = p.tekhelet)
                    }
                    if (x.text.isNotBlank()) Text(x.text, style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun Transcricao(subs: Subs?, cur: Int, lang: String, error: Boolean, seek: (Double) -> Unit) {
    val p = LocalPalette.current
    if (subs == null) {
        if (error) Centered("Não foi possível carregar a transcrição agora.") else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val state = rememberLazyListState()
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(cur, follow) { if (follow && cur >= 0) state.animateScrollToItem((cur - 2).coerceAtLeast(0)) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${subs.cues.size} linhas", style = MaterialTheme.typography.bodySmall, color = p.muted, modifier = Modifier.weight(1f))
            Text(if (follow) "Seguindo a fala ✓" else "Seguir a fala", style = MaterialTheme.typography.labelLarge, color = p.tekhelet, modifier = Modifier.clickable { follow = !follow }.padding(6.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(subs.cues) { i, cue ->
                val tr = if (lang == "en") subs.en?.getOrNull(i) else subs.pt?.getOrNull(i)
                Row(
                    Modifier.fillMaxWidth().background(if (i == cur) p.wash else Color.Transparent).clickable { seek(cue.start) }.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(clock(cue.start), style = MaterialTheme.typography.labelSmall, color = if (i == cur) p.tekhelet else p.muted, modifier = Modifier.width(52.dp).padding(top = 3.dp))
                    Column(Modifier.weight(1f)) {
                        HebrewText(cue.he, 17, p.ink, Modifier.fillMaxWidth(), weight = if (i == cur) FontWeight.Bold else FontWeight.Normal)
                        if (lang != "he" && !tr.isNullOrBlank()) Text(tr, style = MaterialTheme.typography.bodyMedium, color = if (i == cur) p.ink else p.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
