package com.skintracker.app.data

/** Находка для перекупа: где купить дешевле всего и где выгоднее продать (после комиссии). */
data class FlipRow(
    val name: String,
    val buy: PlatPrice,
    val sell: PlatPrice,
    val profit: Double,
    val pct: Double,
    val all: List<PlatPrice>
)

/**
 * Ищет по всем скинам, где цена на площадках заметно отличается.
 * Отсеивает подозрительное: слишком дорогие/дешёвые разницы, редкие скины и одиночные «странные» лоты.
 */
fun computeFlips(c: Catalogs, s: Settings): List<FlipRow> {
    val usable = listOf(Platforms.SKINPORT, Platforms.CSGO, Platforms.WAXPEER).filter { it in s.platforms }
    if (usable.size < 2) return emptyList()
    val names = HashSet<String>(c.skinport.size + c.csgo.size + c.wax.size)
    if (Platforms.SKINPORT in usable) names.addAll(c.skinport.keys)
    if (Platforms.CSGO in usable) names.addAll(c.csgo.keys)
    if (Platforms.WAXPEER in usable) names.addAll(c.wax.keys)

    val out = ArrayList<FlipRow>()
    for (n in names) {
        val list = ArrayList<PlatPrice>(3)
        for (id in usable) {
            var p: Double? = null
            var q = 0
            when (id) {
                Platforms.SKINPORT -> c.skinport[n]?.let { p = it.minPrice; q = it.quantity }
                Platforms.CSGO -> c.csgo[n]?.let { p = it.price; q = it.qty }
                else -> c.wax[n]?.let { p = it.price; q = it.qty }
            }
            val price = p
            if (price != null && price > 0.0) {
                list.add(PlatPrice(id, Platforms.title(id), price, q, price * (1 - s.fee(id) / 100.0)))
            }
        }
        if (list.size < 2) continue
        val buy = list.minByOrNull { it.ask } ?: continue
        if (buy.ask < 1.0 || buy.qty < 1) continue
        val sell = list.filter { it.id != buy.id }.maxByOrNull { it.net } ?: continue
        if (sell.qty < 5) continue
        val profit = sell.net - buy.ask
        val pct = profit / buy.ask * 100.0
        // слишком большая «выгода» почти всегда значит ошибку: другой скин, битый лот, приманка
        if (pct < s.profitPct || pct > 45.0) continue
        // если остальные площадки сильно расходятся между собой, цене верить нельзя
        val others = list.filter { it.id != buy.id }.map { it.ask }.sorted()
        if (others.size >= 2 && others.last() > others.first() * 1.25) continue
        out.add(FlipRow(n, buy, sell, profit, pct, list.sortedBy { it.ask }))
    }
    out.sortByDescending { it.pct }
    return out.take(800)
}
