package io.github.nowaaak.quietv.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

class LightEdge(
    private val radius: Float,
    private val fill: Int,
    private val edge: Float,
) : Drawable() {

    private val box = RectF()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = edge
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        box.set(bounds)
        edgePaint.shader = LinearGradient(
            box.left, box.top, box.right, box.bottom,
            intArrayOf(white(BRIGHT), white(DIM), white(DIM), white(BACK)),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        if (box.isEmpty) return
        val r = minOf(radius, box.height() / 2f, box.width() / 2f)
        if (Color.alpha(fill) > 0) canvas.drawRoundRect(box, r, r, fillPaint)
        val half = edge / 2f
        canvas.drawRoundRect(box.left + half, box.top + half, box.right - half, box.bottom - half, r - half, r - half, edgePaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun white(alpha: Int): Int = Color.argb(alpha, 255, 255, 255)

    private companion object {
        const val BRIGHT = 0xD9
        const val DIM = 0x1C
        const val BACK = 0x8C
    }
}
