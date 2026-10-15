package br.com.shiurhebraico

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.launch

class ShiurApp : Application(), ImageLoaderFactory {
    val repo by lazy { Repo(this) }
    val progress by lazy { Progress(this) }

    /** stills and portraits fade in, and stay cached on disk between visits */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(180)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(150L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .build()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ShiurApp
        setContent {
            ShiurTheme { Root(app.repo, app.progress) }
        }
    }
}

private data class RootTab(val label: String, val on: ImageVector, val off: ImageVector, val key: String)

private val ROOT_TABS = listOf(
    RootTab("Início", Icons.Filled.Home, Icons.Outlined.Home, "home"),
    RootTab("Mestres", Icons.Filled.People, Icons.Outlined.People, "mestres"),
    RootTab("Parashiot", Icons.Filled.AutoStories, Icons.Outlined.AutoStories, "parashiot"),
)

private const val EASE = 300

@Composable
private fun Root(repo: Repo, progress: Progress) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun load() {
        failed = false
        scope.launch {
            if (catalog == null) catalog = repo.cachedCatalog()
            runCatching { repo.freshCatalog() }.onSuccess { catalog = it }.onFailure { if (catalog == null) failed = true }
        }
    }
    LaunchedEffect(Unit) { load() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // the pages opened on top of the tabs: "aula/<id>", "rav/<slug>", "parasha/<key>"
    val stack = remember { mutableStateListOf<String>() }
    // each page keeps its scroll and choices while another is on top (and the tabs keep theirs)
    val saved = rememberSaveableStateHolder()
    var pushing by remember { mutableStateOf(true) }
    fun pop() {
        if (stack.isEmpty()) return
        pushing = false
        val gone = stack.removeAt(stack.lastIndex)
        if (gone !in stack) saved.removeState(gone)
    }
    fun push(route: String, replaceKind: String? = null) {
        pushing = true
        if (replaceKind != null && stack.lastOrNull()?.startsWith(replaceKind) == true) {
            val gone = stack.last()
            stack[stack.lastIndex] = route
            if (gone !in stack) saved.removeState(gone)
        } else {
            stack.add(route)
        }
    }
    BackHandler(enabled = stack.isNotEmpty() || tab != 0) {
        if (stack.isNotEmpty()) pop() else { pushing = false; tab = 0 }
    }
    val nav = remember {
        Nav(
            lesson = { id -> push("aula/$id", replaceKind = "aula/") },
            rav = { push("rav/$it") },
            parasha = { k -> push("parasha/$k", replaceKind = "parasha/") },
            back = { pop() },
        )
    }

    val c = catalog
    Box(Modifier.fillMaxSize().background(p.paper)) {
        when {
            c == null && failed -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sem conexão com o Shiur Hebraico.", style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center)
                Button(onClick = { load() }, modifier = Modifier.padding(top = 16.dp)) { Text("Tentar de novo") }
            }
            c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.tekhelet) }
            else -> {
                val route = stack.lastOrNull() ?: "tab/${ROOT_TABS[tab].key}"
                val onTab = stack.isEmpty()
                Column(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = route,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            val tabSwitch = initialState.startsWith("tab/") && targetState.startsWith("tab/")
                            when {
                                tabSwitch -> fadeIn(tween(160, delayMillis = 40)) togetherWith fadeOut(tween(120))
                                pushing -> (slideInHorizontally(tween(EASE, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(EASE))) togetherWith
                                    (slideOutHorizontally(tween(EASE, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(EASE / 2)))
                                else -> (slideInHorizontally(tween(EASE, easing = FastOutSlowInEasing)) { -it / 8 } + fadeIn(tween(EASE))) togetherWith
                                    (slideOutHorizontally(tween(EASE, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(EASE / 2)))
                            }.let { ContentTransform(it.targetContentEnter, it.initialContentExit, targetContentZIndex = if (pushing) 1f else 0f) }
                        },
                        label = "nav",
                    ) { r ->
                        saved.SaveableStateProvider(r) {
                            val arg = r.substringAfter('/')
                            Box(Modifier.fillMaxSize().background(p.paper).then(if (r.startsWith("tab/")) Modifier else Modifier.navigationBarsPadding())) {
                                when {
                                    r == "tab/home" -> HomeScreen(c, progress, nav)
                                    r == "tab/mestres" -> MestresScreen(c, nav)
                                    r == "tab/parashiot" -> ParashiotScreen(c, nav)
                                    r.startsWith("aula/") -> AulaScreen(c, arg, repo, progress, nav)
                                    r.startsWith("rav/") -> RavScreen(c, arg, progress, nav)
                                    else -> ParashaScreen(c, arg, nav)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(onTab, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        NavigationBar(containerColor = p.surface, tonalElevation = 0.dp) {
                            ROOT_TABS.forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = {
                                        if (tab != i) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tab = i
                                    },
                                    icon = { Icon(if (tab == i) t.on else t.off, t.label) },
                                    label = { Text(t.label) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = p.tekhelet, selectedTextColor = p.tekhelet, indicatorColor = p.wash, unselectedIconColor = p.muted, unselectedTextColor = p.muted),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
