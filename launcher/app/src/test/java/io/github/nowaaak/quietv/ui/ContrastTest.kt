package io.github.nowaaak.quietv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val graphite = 0xFF0E1014.toInt()
    private val lightText = 0xFFF3F4F6.toInt()
    private val mutedText = 0xFF9AA1AD.toInt()

    @Test
    fun blackAndWhiteHaveTheFullRatio() {
        assertEquals(21.0, Contrast.ratio(white, black), 0.01)
        assertEquals(1.0, Contrast.ratio(white, white), 0.01)
    }

    @Test
    fun blendMovesTowardsTheOverlay() {
        assertEquals(white, Contrast.blend(white, black, 0f))
        assertEquals(black, Contrast.blend(white, black, 1f))
        assertEquals(0xFF7F7F7F.toInt(), Contrast.blend(white, black, 0.5f))
    }

    @Test
    fun darkWallpaperKeepsTheChosenDimming() {
        val samples = IntArray(144) { 0xFF101418.toInt() }
        assertEquals(0.3f, Contrast.scrimFor(samples, intArrayOf(lightText, mutedText), graphite, 0.3f), 0.001f)
    }

    @Test
    fun brightWallpaperIsDarkenedUntilTextIsReadable() {
        val samples = IntArray(144) { 0xFFEEEEEE.toInt() }
        val amount = Contrast.scrimFor(samples, intArrayOf(lightText, mutedText), graphite, 0.3f)
        assertTrue("amount $amount", amount > 0.3f)
        val result = Contrast.blend(0xFFEEEEEE.toInt(), graphite, amount)
        assertTrue(Contrast.ratio(mutedText, result) >= Contrast.TARGET)
    }

    @Test
    fun aFewBrightSpotsDoNotDarkenTheWholeImage() {
        val samples = IntArray(144) { if (it < 5) white else 0xFF101418.toInt() }
        assertEquals(0.2f, Contrast.scrimFor(samples, intArrayOf(lightText, mutedText), graphite, 0.2f), 0.001f)
    }

    @Test
    fun withAlphaKeepsTheColour() {
        assertEquals(0x80123456.toInt(), Contrast.withAlpha(0xFF123456.toInt(), 0x80))
    }
}
