package com.skintracker.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skintracker.app.ui.AddScreen
import com.skintracker.app.ui.DetailScreen
import com.skintracker.app.ui.HomeScreen
import com.skintracker.app.ui.LocalRub
import com.skintracker.app.ui.PlatformsDialog
import com.skintracker.app.ui.SettingsDialog
import com.skintracker.app.ui.SkinTheme
import com.skintracker.app.ui.TradesScreen
import com.skintracker.app.ui.BottomNav
import com.skintracker.app.ui.CalcScreen
import com.skintracker.app.ui.FeedScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val st by vm.state.collectAsStateWithLifecycle()
            SkinTheme(st.settings.theme) {
                Root(vm)
            }
        }
    }
}

@Composable
private fun Root(vm: MainViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val icons by vm.icons.collectAsStateWithLifecycle()
    val doneFor by vm.doneFor.collectAsStateWithLifecycle()
    val catalogs by vm.catalogs.collectAsStateWithLifecycle()
    val previewSales by vm.previewSales.collectAsStateWithLifecycle()
    val previewLoading by vm.previewLoading.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    var adding by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var platformsOpen by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val m = message
        if (m != null) {
            val r = snackbar.showSnackbar(
                message = m.text,
                actionLabel = m.action,
                duration = if (m.action != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) m.onAction?.invoke()
            vm.clearMessage()
        }
    }

    BackHandler(enabled = adding || detail != null || tab != 0) {
        if (adding) adding = false else if (detail != null) detail = null else tab = 0
    }
    val flips by vm.flips.collectAsStateWithLifecycle()
    val bottom: @Composable () -> Unit = { BottomNav(tab) { tab = it } }

    val rate = if (st.settings.showRub) st.settings.rubRate else 0.0

    CompositionLocalProvider(LocalRub provides rate) {
        when {
            adding -> AddScreen(
                tracked = st.items.map { it.name }.toSet(),
                settings = st.settings,
                results = results,
                icons = icons,
                searching = searching,
                doneFor = doneFor,
                catalogs = catalogs,
                previewSales = previewSales,
                previewLoading = previewLoading,
                snackbar = snackbar,
                onQuery = vm::search,
                onMoreIcons = vm::moreIcons,
                onWarm = vm::warmCatalog,
                onNeedSales = vm::loadPreviewSales,
                onAdd = vm::add,
                onBack = { adding = false }
            )
            detail != null -> DetailScreen(
                t = st.items.firstOrNull { it.name == detail },
                settings = st.settings,
                onBack = { detail = null },
                onPosition = { buy, sell, buyTarget -> detail?.let { vm.setPosition(it, buy, sell, buyTarget) } },
                onSold = { got -> detail?.let { vm.sold(it, got) } },
                history = detail?.let { history[it] },
                onNeedHistory = { detail?.let { vm.loadHistory(it) } },
                onDelete = {
                    detail?.let { vm.remove(it) }
                    detail = null
                }
            )
            tab == 1 -> FeedScreen(
                flips = flips,
                loading = catalogs.isEmpty(),
                settings = st.settings,
                tracked = st.items.map { it.name }.toSet(),
                icons = icons,
                snackbar = snackbar,
                onWarm = vm::warmCatalog,
                onAdd = vm::add,
                bottomBar = bottom
            )
            tab == 2 -> CalcScreen(settings = st.settings, bottomBar = bottom)
            tab == 3 -> TradesScreen(trades = st.trades, onDelete = vm::removeTrade, bottomBar = bottom)
            else -> HomeScreen(
                st = st,
                refreshing = refreshing,
                snackbar = snackbar,
                onRefresh = vm::refresh,
                onAdd = { adding = true },
                onOpen = { detail = it },
                onSettings = { settingsOpen = true },
                onPlatforms = { platformsOpen = true },
                onTrades = { tab = 3 },
                onFeed = { tab = 1 },
                bottomBar = bottom
            )
        }

        if (platformsOpen) {
            PlatformsDialog(
                s = st.settings,
                onDismiss = { platformsOpen = false },
                onSave = {
                    vm.savePlatforms(it)
                    platformsOpen = false
                }
            )
        }
        if (settingsOpen) {
            SettingsDialog(
                s = st.settings,
                onDismiss = { settingsOpen = false },
                onSave = {
                    vm.saveSettings(it)
                    settingsOpen = false
                }
            )
        }
    }
}
