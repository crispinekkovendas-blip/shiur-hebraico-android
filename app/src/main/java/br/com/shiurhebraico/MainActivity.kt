package br.com.shiurhebraico

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class ShiurApp : Application() {
    val repo by lazy { Repo(this) }
    val progress by lazy { Progress(this) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ShiurApp
        setContent {
            ShiurTheme { Root(app.repo, app.progress) }
        }
    }
}

private val ROOT_TABS = listOf(
    Triple("Início", Icons.Filled.Home, "home"),
    Triple("Mestres", Icons.Filled.People, "mestres"),
    Triple("Parashiot", Icons.Filled.AutoStories, "parashiot"),
)

@Composable
private fun Root(repo: Repo, progress: Progress) {
    val p = LocalPalette.current
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
    BackHandler(enabled = stack.isNotEmpty() || tab != 0) { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else tab = 0 }
    val nav = remember {
        Nav(
            lesson = { id -> if (stack.lastOrNull()?.startsWith("aula/") == true) stack[stack.lastIndex] = "aula/$id" else stack.add("aula/$id") },
            rav = { stack.add("rav/$it") },
            parasha = { k -> if (stack.lastOrNull()?.startsWith("parasha/") == true) stack[stack.lastIndex] = "parasha/$k" else stack.add("parasha/$k") },
            back = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) },
        )
    }

    val c = catalog
    Box(Modifier.fillMaxSize().background(p.paper)) {
        when {
            c == null && failed -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sem conexão com o Shiur Hebraico.", style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center)
                Button(onClick = { load() }, modifier = Modifier.padding(top = 16.dp)) { Text("Tentar de novo") }
            }
            c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.tekhelet) }
            stack.isNotEmpty() -> {
                val top = stack.last()
                // a new page for each route, so a lesson's player and state start fresh
                key(top) {
                    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                        val arg = top.substringAfter('/')
                        when {
                            top.startsWith("aula/") -> AulaScreen(c, arg, repo, progress, nav)
                            top.startsWith("rav/") -> RavScreen(c, arg, progress, nav)
                            else -> ParashaScreen(c, arg, nav)
                        }
                    }
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (ROOT_TABS[tab].third) {
                        "home" -> HomeScreen(c, progress, nav)
                        "mestres" -> MestresScreen(c, nav)
                        else -> ParashiotScreen(c, nav)
                    }
                }
                NavigationBar(containerColor = p.surface, tonalElevation = 0.dp) {
                    ROOT_TABS.forEachIndexed { i, (label, icon, _) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = p.tekhelet, selectedTextColor = p.tekhelet, indicatorColor = p.wash, unselectedIconColor = p.muted, unselectedTextColor = p.muted),
                        )
                    }
                }
            }
        }
    }
}
