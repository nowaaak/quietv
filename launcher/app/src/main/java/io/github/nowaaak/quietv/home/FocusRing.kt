package io.github.nowaaak.quietv.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.ui.Palette

class FocusRing(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = RING_GAP_DP * density
    }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private var movingColor = 0
    private val rect = RectF()
    private var accent = LauncherConfig.DEFAULT_ACCENT
    private var moving = false
    private var radius = 20f * density

    init {
        rimPaint.color = accent
    }

    fun setAccent(color: Int) {
        accent = color
        if (!moving) rimPaint.color = color
        invalidate()
    }

    fun setPalette(palette: Palette) {
        gapPaint.color = palette.background
        movingColor = palette.text
        if (moving) rimPaint.color = movingColor
        invalidate()
    }

    fun setMoving(value: Boolean) {
        moving = value
        rimPaint.color = if (value) movingColor else accent
        invalidate()
    }

    fun setRadius(px: Float) {
        radius = px
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val inset = INSET_DP * density
        rect.set(inset, inset, width - inset, height - inset)

        val gap = gapPaint.strokeWidth / 2f
        canvas.drawRoundRect(
            rect.left - gap, rect.top - gap, rect.right + gap, rect.bottom + gap,
            radius + gap, radius + gap, gapPaint,
        )

        val ring = gapPaint.strokeWidth + rimPaint.strokeWidth / 2f
        val r = radius + ring
        canvas.drawRoundRect(rect.left - ring, rect.top - ring, rect.right + ring, rect.bottom + ring, r, r, rimPaint)
    }

    companion object {
        const val INSET_DP = 12f
        private const val RING_GAP_DP = 3f
    }
}
