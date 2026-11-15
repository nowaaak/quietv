package io.github.nowaaak.quietv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.view.KeyEvent
import android.view.View

/**
 * A horizontal slider for whole numbers, made for the remote: left and right move it, holding the
 * key speeds it up. The track is either a colour gradient (for colour channels) or a plain line that
 * fills up to the thumb.
 */
class ValueSlider(context: Context) : View(context) {

    var min = 0
    var max = 100
    var step = 1
    var focusColor = Color.WHITE
    var thumbColor = Color.WHITE
    var lineColor = Contrast.withAlpha(Color.WHITE, 0x40)
    var fillColor = Color.WHITE
    var onChange: ((Int) -> Unit)? = null

    /** Colours the track runs through, left to right. Null draws a plain track filled up to the thumb. */
    var trackColors: IntArray? = null
        set(value) {
            field = value
            updateTrack()
            invalidate()
        }

    var value = 0
        private set

    private val density = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    init {
        isFocusable = true
        isClickable = true
        layoutDirection = LAYOUT_DIRECTION_LTR
    }

    /** Moves the thumb without calling [onChange]. */
    fun show(newValue: Int) {
        value = newValue.coerceIn(min, max)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTrack()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val sign = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1
            else -> return super.onKeyDown(keyCode, event)
        }
        // Holding the key speeds up after a moment, so a full sweep never takes long.
        val amount = step * if (event.repeatCount > FAST_AFTER) FAST else 1
        val next = (value + sign * amount).coerceIn(min, max)
        if (next != value) {
            value = next
            invalidate()
            onChange?.invoke(next)
        }
        return true
    }

    private fun updateTrack() {
        val colors = trackColors
        track.shader = if (colors == null || width == 0) {
            null
        } else {
            LinearGradient(inset(), 0f, width - inset(), 0f, colors, null, Shader.TileMode.CLAMP)
        }
    }

    private fun inset(): Float = height / 2f

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f
        val half = TRACK_DP * density / 2f
        val start = inset()
        val end = width - inset()
        val fraction = if (max > min) (value - min).toFloat() / (max - min) else 0f
        val x = start + (end - start) * fraction
        if (trackColors != null) {
            canvas.drawRoundRect(start - half, cy - half, end + half, cy + half, half, half, track)
        } else {
            track.color = lineColor
            canvas.drawRoundRect(start - half, cy - half, end + half, cy + half, half, half, track)
            track.color = fillColor
            canvas.drawRoundRect(start - half, cy - half, x + half, cy + half, half, half, track)
        }
        val radius = height / 2f - RING_DP * density
        thumb.color = thumbColor
        canvas.drawCircle(x, cy, radius, thumb)
        ring.strokeWidth = RING_DP * density
        ring.color = if (isFocused) focusColor else Contrast.withAlpha(Color.WHITE, IDLE_RING_ALPHA)
        canvas.drawCircle(x, cy, radius, ring)
    }

    private companion object {
        const val TRACK_DP = 10f
        const val RING_DP = 3f
        const val IDLE_RING_ALPHA = 0xB3
        const val FAST_AFTER = 6
        const val FAST = 4
    }
}
