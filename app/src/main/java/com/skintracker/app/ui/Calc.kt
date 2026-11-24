@file:OptIn(ExperimentalMaterial3Api::class)

package com.skintracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skintracker.app.data.Platforms
import com.skintracker.app.data.Settings
import com.skintracker.app.data.fmtMoney
import com.skintracker.app.data.fmtPct
import com.skintracker.app.data.fmtSignedMoney

private fun num(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

/** Калькулятор: за сколько купил, за сколько продашь — сколько останется на каждой площадке. */
@Composable
fun CalcScreen(settings: Settings, bottomBar: @Composable () -> Unit) {
    var buy by rememberSaveable { mutableStateOf("") }
    var sell by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("1") }
    var want by rememberSaveable { mutableStateOf(settings.profitPct.toString()) }
    val kb = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val b = num(buy)
    val sl = num(sell)
    val n = (num(count) ?: 1.0).coerceAtLeast(1.0)
    val w = num(want) ?: 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Расчёт прибыли", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.bg)
            )
        },
        bottomBar = bottomBar
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Впиши цены в долларах — покажу, сколько останется на руках на каждой площадке (комиссия уже вычтена).",
                color = C.muted, fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = buy, onValueChange = { buy = it }, singleLine = true,
                    label = { Text("Купил за, \$") }, keyboardOptions = kb, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sell, onValueChange = { sell = it }, singleLine = true,
                    label = { Text("Продам за, \$") }, keyboardOptions = kb, modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = count, onValueChange = { count = it }, singleLine = true,
                    label = { Text("Сколько штук") }, keyboardOptions = kb, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = want, onValueChange = { want = it }, singleLine = true,
                    label = { Text("Хочу заработать, %") }, keyboardOptions = kb, modifier = Modifier.weight(1f)
                )
            }

            if (b == null || b <= 0.0) {
                Text("Введи цену покупки, чтобы увидеть расчёт.", color = C.muted)
            } else {
                var bestId: String? = null
                var bestVal = Double.NEGATIVE_INFINITY
                if (sl != null) {
                    for (p in Platforms.all) {
                        val v = sl * (1 - settings.fee(p.id) / 100.0) - b
                        if (v > bestVal) {
                            bestVal = v
                            bestId = p.id
                        }
                    }
                }
                Platforms.all.forEach { p ->
                    val fee = settings.fee(p.id) / 100.0
                    val net = if (sl != null) sl * (1 - fee) else null
                    val profit = if (net != null) (net - b) * n else null
                    val pct = if (net != null) (net - b) / b * 100.0 else null
                    val breakEven = b / (1 - fee)
                    val target = b * (1 + w / 100.0) / (1 - fee)
                    val col = if (profit == null) C.muted else if (profit >= 0) C.up else C.down
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (p.id == bestId) C.card2 else C.card)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.title, fontWeight = FontWeight.Bold)
                                Text(
                                    "комиссия ${settings.fee(p.id)}" + (if (p.id == bestId) "% · выгоднее всего" else "%"),
                                    color = C.muted, fontSize = 12.sp
                                )
                            }
                            if (profit != null && pct != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(fmtSignedMoney(profit) + " (" + fmtPct(pct) + ")", color = col, fontWeight = FontWeight.Bold)
                                    RubLine(profit, signed = true, color = col)
                                }
                            }
                        }
                        if (net != null) {
                            Text("На руки с одной штуки: ${fmtMoney(net)}", color = C.muted, fontSize = 12.sp)
                        }
                        Text("Чтобы выйти в ноль, продавай от ${fmtMoney(breakEven)}", color = C.muted, fontSize = 12.sp)
                        Text(
                            "Чтобы заработать ${w.toInt()}%, продавай от ${fmtMoney(target)}",
                            color = C.muted, fontSize = 12.sp
                        )
                    }
                }
                Text(
                    "Комиссии примерные. Их можно поправить на главном экране: «Площадки».",
                    color = C.muted, fontSize = 11.sp
                )
            }
        }
    }
}
