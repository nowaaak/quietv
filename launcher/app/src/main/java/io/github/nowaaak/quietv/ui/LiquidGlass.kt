package io.github.nowaaak.quietv.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

class LiquidGlass(
    private val radius: Float,
    private val tint: Int,
    private val rim: Float,
    private val focusedFill: Int? = null,
    private val sheen: Boolean = true,
) : Drawable() {

    private var backdrop: Bitmap? = null
    private val backdropRect = RectF()
    private var focused = false
    private val box = RectF()
    private val matrix = Matrix()
    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun setBackdrop(bitmap: Bitmap?, region: RectF) {
        backdrop = bitmap
        backdropRect.set(region)
        updateShaders()
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updateShaders()
    }

    override fun isStateful(): Boolean = focusedFill != null

    override fun onStateChange(state: IntArray): Boolean {
        val now = state.contains(android.R.attr.state_focused)
        if (now == focused) return false
        focused = now
        invalidateSelf()
        return true
    }

    private fun updateShaders() {
        val b = bounds
        if (b.isEmpty) return
        box.set(b)
        val h = b.height().toFloat()
        sheenPaint.shader = LinearGradient(
            0f, b.top.toFloat(), 0f, b.top + h * SHEEN_DEPTH,
            Color.argb(SHEEN_ALPHA, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
        glowPaint.shader = LinearGradient(
            0f, b.bottom.toFloat(), 0f, b.bottom - h * GLOW_DEPTH,
            Color.argb(GLOW_ALPHA, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
        rimPaint.strokeWidth = rim
        rimPaint.shader = LinearGradient(
            b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(),
            intArrayOf(
                Color.argb(RIM_BRIGHT, 255, 255, 255),
                Color.argb(RIM_DIM, 255, 255, 255),
                Color.argb(RIM_DIM, 255, 255, 255),
                Color.argb(RIM_BACK, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        innerRimPaint.strokeWidth = rim
        innerRimPaint.color = Color.argb(INNER_RIM_ALPHA, 255, 255, 255)
        tintPaint.color = tint

        val image = backdrop
        if (image == null || backdropRect.isEmpty) {
            backdropPaint.shader = null
            return
        }
        val lensX = backdropRect.width() * LENS
        val lensY = backdropRect.height() * LENS
        val src = RectF(
            backdropRect.left + lensX, backdropRect.top + lensY,
            backdropRect.right - lensX, backdropRect.bottom - lensY,
        )
        matrix.setRectToRect(src, box, Matrix.ScaleToFit.FILL)
        backdropPaint.shader = BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(matrix)
        }
    }

    override fun draw(canvas: Canvas) {
        if (box.isEmpty) return
        val r = minOf(radius, box.height() / 2f, box.width() / 2f)
        val fill = focusedFill
        if (focused && fill != null) {
            tintPaint.color = fill
            canvas.drawRoundRect(box, r, r, tintPaint)
            tintPaint.color = tint
        } else {
            if (backdropPaint.shader != null) canvas.drawRoundRect(box, r, r, backdropPaint)
            canvas.drawRoundRect(box, r, r, tintPaint)
            if (sheen) {
                canvas.drawRoundRect(box, r, r, sheenPaint)
                canvas.drawRoundRect(box, r, r, glowPaint)
            }
        }
        val half = rim / 2f
        canvas.drawRoundRect(
            box.left + half, box.top + half, box.right - half, box.bottom - half,
            r - half, r - half, rimPaint,
        )
        if (!focused && sheen) {
            val inset = rim * 2.5f
            canvas.drawRoundRect(
                box.left + inset, box.top + inset, box.right - inset, box.bottom - inset,
                r - inset, r - inset, innerRimPaint,
            )
        }
    }

    override fun setAlpha(alpha: Int) {
        tintPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        tintPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val LENS = 0.08f
        const val SHEEN_DEPTH = 0.55f
        const val SHEEN_ALPHA = 0x30
        const val GLOW_DEPTH = 0.35f
        const val GLOW_ALPHA = 0x18
        const val RIM_BRIGHT = 0xC0
        const val RIM_DIM = 0x22
        const val RIM_BACK = 0x80
        const val INNER_RIM_ALPHA = 0x14
    }
}
