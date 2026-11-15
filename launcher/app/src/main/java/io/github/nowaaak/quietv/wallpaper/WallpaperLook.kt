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
        val dim = dimPercent / 100f
        // What the text needs to stay readable on this picture. On a bright picture this is more
        // than any dimming setting, so it may only decide where text actually is.
        val readable = Contrast.scrimFor(samples, texts, palette.background, dim)
        val target = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = target.width.toFloat()
        val h = target.height.toFloat()
        val paint = Paint()
        if (palette.glass) {
            // Readable behind the clock and the tiles. In the middle, where only the picture is,
            // the dimming setting decides, so the slider always visibly changes the picture.
            val colors = GLASS_TEXT_WEIGHT.map { weight ->
                val amount = dim + (readable - dim) * weight
                Contrast.withAlpha(palette.background, (amount * 255).roundToInt())
            }
            paint.shader = LinearGradient(0f, 0f, 0f, h, colors.toIntArray(), null, Shader.TileMode.CLAMP)
            paint.isDither = true
        } else {
            paint.color = Contrast.withAlpha(palette.background, (readable * 255).roundToInt())
        }
        Canvas(target).drawRect(0f, 0f, w, h, paint)
        return target
    }

    // How much each band, top to bottom, needs readable text: clock, sky, middle, tile names, tiles.
    private val GLASS_TEXT_WEIGHT = listOf(1f, 0.45f, 0f, 0.7f, 1f)
}
