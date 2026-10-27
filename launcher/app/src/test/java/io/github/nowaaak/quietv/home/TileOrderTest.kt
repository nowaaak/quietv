package io.github.nowaaak.quietv.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TileOrderTest {

    private data class App(val pkg: String, val label: String)

    private val apps = listOf(
        App("com.z.zattoo", "Zattoo"),
        App("com.netflix.ninja", "Netflix"),
        App("com.a.arte", "arte"),
        App("com.google.youtube", "YouTube"),
    )

    private fun sort(saved: List<String>, favorites: List<String>) =
        TileOrder.sort(apps, saved, favorites, key = { it.pkg }, label = { it.label }).map { it.label }

    @Test
    fun withoutPreferencesTilesAreAlphabeticalIgnoringCase() {
        assertEquals(listOf("arte", "Netflix", "YouTube", "Zattoo"), sort(emptyList(), emptyList()))
    }

    @Test
    fun favoritesComeFirstInTheirOrder() {
        assertEquals(
            listOf("YouTube", "Netflix", "arte", "Zattoo"),
            sort(emptyList(), listOf("com.google.youtube", "com.netflix.ninja", "com.missing.app")),
        )
    }

    @Test
    fun savedOrderWinsOverFavorites() {
        assertEquals(
            listOf("Zattoo", "arte", "YouTube", "Netflix"),
            sort(listOf("com.z.zattoo", "com.a.arte"), listOf("com.google.youtube")),
        )
    }
}
