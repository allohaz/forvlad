package com.skintracker.app.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

private val TABS = listOf(
    Triple("Скины", "★", 0),
    Triple("Перекупы", "⇄", 1),
    Triple("Расчёт", "＝", 2),
    Triple("Сделки", "✓", 3)
)

/** Нижнее меню: четыре главных раздела. */
@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = C.card) {
        TABS.forEach { (label, glyph, i) ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = { Text(glyph, fontSize = 20.sp) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = C.accent,
                    selectedTextColor = C.accent,
                    unselectedIconColor = C.muted,
                    unselectedTextColor = C.muted,
                    indicatorColor = C.card2
                )
            )
        }
    }
}
