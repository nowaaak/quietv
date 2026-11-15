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
 * A horizontal slider for one HSV channel, made for the remote: left and right move it, holding the
 * key repeats. The track shows the colours the channel runs through.
 */
class ColorSlider(context: Context) : View(context) {

    enum class Channel(val max: Float, val step: Float) {
        HUE(360f, 5f),
        SATURATION(1f, 0.04f),
        VALUE(1f, 0.04f),
    }

    var channel = Channel.HUE
    var focusColor = Color.WHITE
    var onChange: ((Float) -> Unit)? = null

    var position = 0f
        private set

    private var hsv = Hsv(0f, 1f, 1f)
    private val density = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    init {
        isFocusable = true
        isClickable = true
        layoutDirection = LAYOUT_DIRECTION_LTR
    }

    /** Sets the colour the track is drawn for and moves the thumb to this channel's part of it. */
    fun show(color: Hsv) {
        hsv = color
        position = when (channel) {
            Channel.HUE -> color.hue
            Channel.SATURATION -> color.saturation
            Channel.VALUE -> color.value
        }
        updateTrack()
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
        // Holding the key speeds up after a moment, so a full sweep takes about two seconds.
        val step = channel.step * if (event.repeatCount > FAST_AFTER) FAST else 1
        val next = if (channel == Channel.HUE) {
            ((position + sign * step) % channel.max + channel.max) % channel.max
        } else {
            (position + sign * step).coerceIn(0f, channel.max)
        }
        if (next != position) {
            position = next
            onChange?.invoke(next)
        }
        return true
    }

    private fun updateTrack() {
        if (width == 0) return
        val stops = when (channel) {
            Channel.HUE -> IntArray(HUE_STOPS + 1) { Hsv(it * 360f / HUE_STOPS, 1f, 1f).toColor() }
            Channel.SATURATION -> intArrayOf(hsv.copy(saturation = 0f).toColor(), hsv.copy(saturation = 1f).toColor())
            Channel.VALUE -> intArrayOf(Color.BLACK, hsv.copy(value = 1f).toColor())
        }
        track.shader = LinearGradient(inset(), 0f, width - inset(), 0f, stops, null, Shader.TileMode.CLAMP)
    }

    private fun inset(): Float = height / 2f

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f
        val half = TRACK_DP * density / 2f
        val start = inset()
        val end = width - inset()
        canvas.drawRoundRect(start - half, cy - half, end + half, cy + half, half, half, track)

        val x = start + (end - start) * (position / channel.max)
        val radius = height / 2f - RING_DP * density
        thumb.color = hsv.toColor()
        canvas.drawCircle(x, cy, radius, thumb)
        ring.strokeWidth = RING_DP * density
        ring.color = if (isFocused) focusColor else Contrast.withAlpha(Color.WHITE, IDLE_RING_ALPHA)
        canvas.drawCircle(x, cy, radius, ring)
    }

    private companion object {
        const val HUE_STOPS = 6
        const val TRACK_DP = 10f
        const val RING_DP = 3f
        const val IDLE_RING_ALPHA = 0xB3
        const val FAST_AFTER = 6
        const val FAST = 3
    }
}
