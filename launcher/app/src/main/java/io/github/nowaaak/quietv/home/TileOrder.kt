package io.github.nowaaak.quietv.home

object TileOrder {

    fun <T> sort(
        items: List<T>,
        saved: List<String>,
        favorites: List<String>,
        key: (T) -> String,
        label: (T) -> String,
    ): List<T> {
        val savedIndex = saved.withIndex().associate { it.value to it.index }
        val favoriteIndex = favorites.withIndex().associate { it.value to it.index }
        return items.sortedWith { a, b ->
            compareRanked(savedIndex[key(a)], savedIndex[key(b)])
                ?: compareRanked(favoriteIndex[key(a)], favoriteIndex[key(b)])
                ?: label(a).compareTo(label(b), ignoreCase = true)
        }
    }

    private fun compareRanked(a: Int?, b: Int?): Int? = when {
        a == null && b == null -> null
        a == null -> 1
        b == null -> -1
        else -> a.compareTo(b)
    }
}
