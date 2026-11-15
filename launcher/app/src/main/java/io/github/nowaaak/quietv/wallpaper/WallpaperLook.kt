package io.github.nowaaak.quietv.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Palette
import kotlin.math.roundToInt

/**
 * Darkens (or lightens) a wallpaper so text stays readable, by drawing the scrim into the bitmap
 * itself. The home screen then draws one full-screen picture instead of a picture with a second
 * full-screen layer on top, which weak TV graphics chips notice on every frame.
 */
object WallpaperLook {

    /** Draws the scrim into [bitmap] and returns it, or a mutable copy if [bitmap] cannot be changed. */
    fun dim(bitmap: Bitmap, palette: Palette, dimPercent: Int): Bitmap {
        val samples = WallpaperStore.samples(bitmap)
        val texts = intArrayOf(palette.text, palette.textMuted, palette.textDim)
        val amount = Contrast.scrimFor(samples, texts, palette.background, dimPercent / 100f)
        val alpha = (amount * 255).roundToInt()
        val target = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = target.width.toFloat()
        val h = target.height.toFloat()
        val paint = Paint()
        if (palette.glass) {
            // Strong behind the clock and the tiles, light in the middle, so the picture shows.
            val colors = GLASS_SCRIM.map { Contrast.withAlpha(palette.background, (alpha * it).roundToInt()) }
            paint.shader = LinearGradient(0f, 0f, 0f, h, colors.toIntArray(), null, Shader.TileMode.CLAMP)
            paint.isDither = true
        } else {
            paint.color = Contrast.withAlpha(palette.background, alpha)
        }
        Canvas(target).drawRect(0f, 0f, w, h, paint)
        return target
    }

    private val GLASS_SCRIM = listOf(1f, 0.45f, 0.25f, 0.7f, 1f)
}
