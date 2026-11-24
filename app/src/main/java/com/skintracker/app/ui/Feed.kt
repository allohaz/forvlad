@file:OptIn(ExperimentalMaterial3Api::class)

package com.skintracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skintracker.app.data.Found
import com.skintracker.app.data.FlipRow
import com.skintracker.app.data.Settings
import com.skintracker.app.data.fmtAbsPct
import com.skintracker.app.data.fmtMoney
import com.skintracker.app.data.fmtSignedMoney

private val MIN_PRICES = listOf(1.0, 5.0, 20.0, 100.0)
private val MIN_LABELS = listOf("от \$1", "от \$5", "от \$20", "от \$100")

/** Лента перекупов: скины, которые сейчас заметно дешевле на одной площадке, чем на другой. */
@Composable
fun FeedScreen(
    flips: List<FlipRow>,
    loading: Boolean,
    settings: Settings,
    tracked: Set<String>,
    icons: Map<String, String>,
    snackbar: SnackbarHostState,
    onWarm: () -> Unit,
    onAdd: (Found) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var minIdx by rememberSaveable { mutableStateOf(1) }
    var sortIdx by rememberSaveable { mutableStateOf(0) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { onWarm() }

    val shown = remember(flips, minIdx, sortIdx) {
        val f = flips.filter { it.buy.ask >= MIN_PRICES[minIdx] }
        if (sortIdx == 0) f.sortedByDescending { it.pct } else f.sortedByDescending { it.profit }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Перекупы", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.bg)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Здесь скины, которые на одной площадке дешевле, чем на другой — даже после комиссии. Это быстрые находки по текущим ценам, не гарантия: перед покупкой проверь состояние скина и сколько дней его нельзя продавать.",
                    color = C.muted, fontSize = 13.sp
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MIN_LABELS.forEachIndexed { i, l -> TagChip(l, minIdx == i) { minIdx = i } }
                    TagChip(if (sortIdx == 0) "Сначала по % ▾" else "Сначала по \$ ▾") { sortIdx = 1 - sortIdx }
                }
            }
            if (settings.platforms.count { it != "dmarket" && it != "steam" } < 2) {
                item {
                    Text(
                        "Включи хотя бы две площадки из: Skinport, Market.CSGO, Waxpeer — на главном экране кнопка «Площадки».",
                        color = C.down
                    )
                }
            } else if (loading && flips.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        Text("Сравниваю цены…", color = C.muted, fontSize = 13.sp)
                    }
                }
            } else if (shown.isEmpty()) {
                item {
                    Text(
                        "Сейчас подходящих находок нет. Попробуй другой фильтр цены или загляни позже.",
                        color = C.muted
                    )
                }
            } else {
                item { Text("Найдено: ${shown.size}", color = C.muted, fontSize = 12.sp) }
            }
            items(shown.take(150), key = { it.name }) { r ->
                FlipCard(r, icons[baseOf(r.name)]) { open = r.name }
            }
        }
    }

    val cur = shown.firstOrNull { it.name == open } ?: flips.firstOrNull { it.name == open }
    if (cur != null) {
        FlipSheet(cur, icons[baseOf(cur.name)], cur.name in tracked, onAdd) { open = null }
    }
}

@Composable
private fun FlipCard(r: FlipRow, icon: String?, onClick: () -> Unit) {
    val n = parseName(r.name)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) SkinImage(icon, 56.dp, 12.dp)
        else Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(C.card2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                n.top.removePrefix("StatTrak™ ").removePrefix("Souvenir ").removePrefix("★ ").take(8),
                color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            if (n.top.isNotEmpty()) Text(n.top, color = C.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                n.skin + (if (n.wear != null) " · " + wearShort(n.wear) else ""),
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${r.buy.title} ${fmtMoney(r.buy.ask)} → ${r.sell.title} ${fmtMoney(r.sell.ask)}",
                color = C.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("+" + fmtAbsPct(r.pct), color = C.flip, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(fmtSignedMoney(r.profit), color = C.up, fontSize = 12.sp)
            RubLine(r.profit, signed = true, color = C.up, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FlipSheet(r: FlipRow, icon: String?, already: Boolean, onAdd: (Found) -> Unit, onDismiss: () -> Unit) {
    val n = parseName(r.name)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.bg
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SkinImage(icon, 120.dp, 20.dp) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (n.top.isNotEmpty()) Text(n.top, color = C.muted)
                Text(n.skin, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                if (n.wear != null) Text(n.wear, color = C.muted)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Как заработать", color = C.muted, fontSize = 12.sp)
                Text("1. Купи на ${r.buy.title} за ${fmtMoney(r.buy.ask)}.")
                Text("2. Продай на ${r.sell.title} за ${fmtMoney(r.sell.ask)}.")
                Text("3. После комиссии останется ${fmtMoney(r.sell.net)}.")
                Text(
                    "Заработок: ${fmtSignedMoney(r.profit)} (+${fmtAbsPct(r.pct)})",
                    color = C.up, fontWeight = FontWeight.Bold
                )
                RubLine(r.profit, signed = true, color = C.up)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Где сколько стоит", color = C.muted, fontSize = 12.sp)
                r.all.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.title)
                            Text("в продаже: ${p.qty}", color = C.muted, fontSize = 12.sp)
                        }
                        Text(fmtMoney(p.ask), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "Осторожно: после покупки скин часто нельзя продать до 7 дней, цена за это время может измениться. У разных экземпляров разное состояние (флоат), наклейки и узор — проверь именно свой лот.",
                color = C.muted, fontSize = 12.sp
            )
            Button(
                onClick = {
                    onAdd(Found(r.name, icon))
                    onDismiss()
                },
                enabled = !already,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.accent, contentColor = C.bg)
            ) { Text(if (already) "Уже в списке" else "Следить за этим скином", fontWeight = FontWeight.Bold) }
        }
    }
}
