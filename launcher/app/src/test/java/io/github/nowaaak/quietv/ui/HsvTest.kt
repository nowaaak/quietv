package io.github.nowaaak.quietv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HsvTest {

    @Test
    fun primariesConvert() {
        assertEquals(0xFFFF0000.toInt(), Hsv(0f, 1f, 1f).toColor())
        assertEquals(0xFF00FF00.toInt(), Hsv(120f, 1f, 1f).toColor())
        assertEquals(0xFF0000FF.toInt(), Hsv(240f, 1f, 1f).toColor())
        assertEquals(0xFFFFFFFF.toInt(), Hsv(200f, 0f, 1f).toColor())
        assertEquals(0xFF000000.toInt(), Hsv(200f, 1f, 0f).toColor())
    }

    @Test
    fun hueWrapsAround() {
        assertEquals(Hsv(10f, 0.5f, 0.8f).toColor(), Hsv(370f, 0.5f, 0.8f).toColor())
        assertEquals(Hsv(350f, 0.5f, 0.8f).toColor(), Hsv(-10f, 0.5f, 0.8f).toColor())
    }

    @Test
    fun everyAccentSurvivesARoundTrip() {
        for (color in ThemeColors.DARK_ACCENTS + ThemeColors.LIGHT_ACCENTS + intArrayOf(0xFF808080.toInt())) {
            assertEquals(color, Hsv.of(color).toColor())
        }
    }

    @Test
    fun accentShadesComeInPairs() {
        assertEquals(ThemeColors.DARK_ACCENTS.size, ThemeColors.LIGHT_ACCENTS.size)
        val light = ThemeColors.LIGHT_ACCENTS[9]
        assertEquals(light, ThemeColors.accentForTheme(ThemeColors.DARK_ACCENTS[9], fromLight = false, toLight = true))
        assertEquals(0xFF123456.toInt(), ThemeColors.accentForTheme(0xFF123456.toInt(), fromLight = false, toLight = true))
    }
}
