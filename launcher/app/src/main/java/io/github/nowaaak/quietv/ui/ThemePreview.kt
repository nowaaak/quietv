package io.github.nowaaak.quietv.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * A small home screen in the given colours: clock, top buttons and two rows of tiles with the first
 * one focused. Drawn from shapes only, so a picker can show many of them without loading anything.
 */
class ThemePreview(
    private val palette: Palette,
    private val accent: Int,
    private val tileRadiusDp: Int,
) : Drawable() {

    private val box = RectF()
    private val shape = RectF()
    private val background = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        box.set(bounds)
        background.shader = null
        background.color = palette.background
        glow.shader = null
        if (palette.glass && !box.isEmpty) {
            background.shader = LinearGradient(
                box.left, box.top, box.right, box.bottom,
                palette.background, palette.backgroundEnd, Shader.TileMode.CLAMP,
            )
            if (palette.glow != 0) {
                glow.shader = RadialGradient(
                    box.left + box.width() * GLOW_X, box.top + box.height() * GLOW_Y,
                    maxOf(box.width(), box.height()) * GLOW_RADIUS,
                    palette.glow, Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (box.isEmpty) return
        val w = box.width()
        val h = box.height()
        val corner = w * CORNER
        canvas.drawRoundRect(box, corner, corner, background)
        if (glow.shader != null) canvas.drawRoundRect(box, corner, corner, glow)

        val margin = w * MARGIN
        // Clock and date.
        bar(canvas, box.left + margin, box.top + h * 0.12f, w * 0.2f, h * 0.075f, palette.text)
        bar(canvas, box.left + margin, box.top + h * 0.225f, w * 0.13f, h * 0.035f, palette.textMuted)
        // Top buttons. The status dot uses the accent colour, and on Glass the first button is
        // focused: a white circle with an accent glow.
        val dot = w * 0.016f
        val dotY = box.top + h * 0.155f
        fill.color = palette.textMuted
        canvas.drawCircle(box.right - margin - dot, dotY, dot, fill)
        if (palette.glass) {
            val x = box.right - margin - dot * 5f
            fill.color = Contrast.withAlpha(accent, HALO_ALPHA)
            canvas.drawCircle(x, dotY, dot * 2.3f, fill)
            fill.color = palette.text
            canvas.drawCircle(x, dotY, dot * 1.7f, fill)
        } else {
            canvas.drawCircle(box.right - margin - dot * 4.5f, dotY, dot, fill)
            fill.color = accent
            canvas.drawCircle(box.right - margin - dot * 8f, dotY, dot * 0.8f, fill)
        }

        val gap = w * GAP
        val tileW = (w - 2 * margin - (COLUMNS - 1) * gap) / COLUMNS
        val tileH = tileW * 9f / 16f
        val radius = tileRadiusDp * tileW / REAL_TILE_DP
        val firstRow = box.top + h * 0.42f
        if (!palette.glass) {
            bar(canvas, box.left + margin, firstRow - h * 0.085f, w * 0.1f, h * 0.03f, palette.textMuted)
        }
        for (row in 0 until 2) {
            val top = firstRow + row * (tileH + h * 0.1f)
            for (column in 0 until COLUMNS) {
                val left = box.left + margin + column * (tileW + gap)
                shape.set(left, top, left + tileW, top + tileH)
                tile(canvas, radius, focused = row == 0 && column == 0)
            }
        }
    }

    private fun tile(canvas: Canvas, radius: Float, focused: Boolean) {
        val r = minOf(radius, shape.height() / 2f)
        if (focused && palette.glass) {
            // Glass lifts the focused tile with a shadow instead of a ring.
            val grow = shape.width() * 0.08f
            shape.inset(-grow, -grow * 9f / 16f)
            fill.color = Contrast.withAlpha(Color.BLACK, SHADOW_ALPHA)
            canvas.drawRoundRect(shape.left, shape.top + grow, shape.right, shape.bottom + grow, r, r, fill)
            fill.color = Contrast.blend(palette.surface, Color.WHITE, 0.22f)
        } else {
            fill.color = ThemeColors.surfaceFill(palette)
        }
        canvas.drawRoundRect(shape, r, r, fill)
        if (palette.glass) {
            stroke.strokeWidth = maxOf(1f, shape.width() * 0.02f)
            stroke.color = ThemeColors.edge(palette)
            canvas.drawRoundRect(shape, r, r, stroke)
        }
        val icon = shape.height() * 0.42f
        fill.color = palette.line
        val cx = shape.centerX()
        val cy = shape.centerY()
        canvas.drawRoundRect(cx - icon / 2, cy - icon / 2, cx + icon / 2, cy + icon / 2, icon * 0.25f, icon * 0.25f, fill)
        if (focused && !palette.glass) {
            val width = maxOf(1.5f, shape.width() * 0.045f)
            val offset = width * 1.2f
            stroke.strokeWidth = width
            stroke.color = accent
            canvas.drawRoundRect(
                shape.left - offset, shape.top - offset, shape.right + offset, shape.bottom + offset,
                r + offset, r + offset, stroke,
            )
        }
    }

    private fun bar(canvas: Canvas, left: Float, top: Float, width: Float, height: Float, color: Int) {
        fill.color = color
        canvas.drawRoundRect(left, top, left + width, top + height, height / 2f, height / 2f, fill)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val COLUMNS = 5
        const val REAL_TILE_DP = 148f
        const val CORNER = 0.035f
        const val MARGIN = 0.07f
        const val GAP = 0.03f
        const val SHADOW_ALPHA = 0x55
        const val HALO_ALPHA = 0x99
        const val GLOW_RADIUS = 0.55f
        const val GLOW_X = 0.12f
        const val GLOW_Y = 0.05f
    }
}
