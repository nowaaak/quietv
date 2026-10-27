package io.github.nowaaak.quietv.ui

import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {

    @Test
    fun everyBundledThemeHasReadableText() {
        for (preset in ThemePreset.entries) {
            val p = Palette.of(preset, emptyMap())
            for ((name, color) in listOf("text" to p.text, "textMuted" to p.textMuted, "textDim" to p.textDim)) {
                for ((under, background) in listOf("background" to p.background, "surface" to p.surface)) {
                    val ratio = Contrast.ratio(color, background)
                    assertTrue("${preset.key}: $name on $under is $ratio", ratio >= Contrast.TARGET)
                }
            }
            assertTrue("${preset.key}: focused pill", Contrast.ratio(p.onText, p.text) >= Contrast.TARGET)
            for (accent in ThemeColors.accents(p.light)) {
                val ratio = Contrast.ratio(accent, p.background)
                assertTrue("${preset.key}: accent ${LauncherConfig.formatColor(accent)} is $ratio", ratio >= Contrast.TARGET)
            }
        }
    }

    @Test
    fun overridesReplaceSingleRoles() {
        val p = Palette.of(ThemePreset.GRAPHITE, mapOf(LauncherConfig.COLOR_SURFACE to 0xFF223344.toInt()))
        assertEquals(0xFF223344.toInt(), p.surface)
        assertEquals(ThemePreset.GRAPHITE.background, p.background)
        assertEquals(ThemePreset.GRAPHITE.textDim, p.textDim)
    }

    @Test
    fun customBackgroundDecidesLightOrDark() {
        val light = Palette.of(ThemePreset.GRAPHITE, mapOf(LauncherConfig.COLOR_BACKGROUND to 0xFFFAFAFA.toInt()))
        val dark = Palette.of(ThemePreset.PAPER, mapOf(LauncherConfig.COLOR_BACKGROUND to 0xFF101010.toInt()))
        assertTrue(light.light)
        assertFalse(dark.light)
    }

    @Test
    fun mutedOverrideAlsoSetsDim() {
        val p = Palette.of(ThemePreset.PAPER, mapOf(LauncherConfig.COLOR_TEXT_MUTED to 0xFF444444.toInt()))
        assertEquals(0xFF444444.toInt(), p.textDim)
    }
}
