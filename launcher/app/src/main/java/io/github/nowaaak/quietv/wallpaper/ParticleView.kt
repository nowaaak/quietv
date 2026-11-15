package io.github.nowaaak.quietv.wallpaper

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The "particles" background: large soft lights and small sparks that drift slowly across the screen.
 *
 * Every particle is a radial gradient the GPU draws at screen resolution, so nothing is scaled up or
 * blurred. It redraws at most 30 times a second, only while the home screen is visible, and stands
 * still for a moment after every key press (so focus moves and scrolling get the whole frame), on
 * low-memory devices and when animations are switched off.
 */
class ParticleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val radius: Float,
        val paint: Paint,
        val alpha: Int,
        val wobble: Float,
        val twinkle: Float,
        val phase: Float,
    )

    private val density = resources.displayMetrics.density
    private val particles = mutableListOf<Particle>()
    private var running = false
    private var lastFrame = 0L
    private var seconds = 0f
    private var holdUntil = 0L

    private val moving: Boolean by lazy {
        val activities = context.getSystemService(ActivityManager::class.java)
        ValueAnimator.areAnimatorsEnabled() && activities?.isLowRamDevice != true
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val elapsed = frameTimeNanos - lastFrame
            if (elapsed >= FRAME_NS) {
                lastFrame = frameTimeNanos
                if (SystemClock.uptimeMillis() >= holdUntil) {
                    step(minOf(elapsed, MAX_STEP_NS) / 1e9f)
                    invalidate()
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private var palette: Palette? = null
    private var accent = 0

    /** Builds the particles for these colours. Calling it again with the same colours keeps them in place. */
    fun configure(palette: Palette, accent: Int) {
        if (palette == this.palette && accent == this.accent && particles.isNotEmpty()) return
        this.palette = palette
        this.accent = accent
        build()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        build()
    }

    fun start() {
        if (running || !moving) return
        running = true
        lastFrame = 0L
        Choreographer.getInstance().postFrameCallback(frame)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
    }

    /** Stands still for a moment, so the focus move or scroll after a key press gets the whole device. */
    fun hold() {
        holdUntil = SystemClock.uptimeMillis() + HOLD_MS
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    private fun build() {
        particles.clear()
        invalidate()
        val palette = palette ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val random = Random(SEED)
        val lights = lightColors(palette, accent)
        repeat(LIGHTS) { i ->
            val radius = dp(random.range(LIGHT_MIN_DP, LIGHT_MAX_DP))
            val color = lights[i % lights.size]
            val alpha = if (palette.light) LIGHT_ALPHA_ON_LIGHT else LIGHT_ALPHA
            particles += particle(random, w, h, radius, SPEED_LIGHT_DP, 0f, color, alpha, bokeh = i % 3 == 0)
        }
        val spark = if (palette.light) accent else palette.text
        repeat(SPARKS) {
            val core = dp(random.range(SPARK_MIN_DP, SPARK_MAX_DP))
            particles += particle(
                random, w, h, core * SPARK_HALO, SPEED_SPARK_DP, -dp(SPARK_RISE_DP), spark, SPARK_ALPHA,
                bokeh = false, core = SPARK_CORE,
            )
        }
    }

    private fun particle(
        random: Random,
        w: Float,
        h: Float,
        radius: Float,
        speedDp: Float,
        rise: Float,
        color: Int,
        alpha: Int,
        bokeh: Boolean,
        core: Float = 0f,
    ): Particle {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        val speed = dp(speedDp) * random.range(0.4f, 1f)
        val stops = when {
            core > 0f -> floatArrayOf(0f, core, core * 1.6f, 1f)
            bokeh -> floatArrayOf(0f, 0.72f, 0.86f, 1f)
            else -> floatArrayOf(0f, 0.35f, 0.7f, 1f)
        }
        val colors = when {
            core > 0f -> intArrayOf(Color.WHITE, Color.WHITE, Contrast.withAlpha(Color.WHITE, 0x40), Color.TRANSPARENT)
            bokeh -> intArrayOf(white(0xB0), Color.WHITE, white(0x80), Color.TRANSPARENT)
            else -> intArrayOf(Color.WHITE, white(0xA0), white(0x38), Color.TRANSPARENT)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // A white gradient tinted with the particle colour, so one shader shape serves every colour.
            shader = RadialGradient(0f, 0f, radius, colors.map { tint(it, color) }.toIntArray(), stops, Shader.TileMode.CLAMP)
            this.alpha = alpha
        }
        return Particle(
            x = random.nextFloat() * w,
            y = random.nextFloat() * h,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed * VERTICAL + rise * random.range(0.5f, 1f),
            radius = radius,
            paint = paint,
            alpha = alpha,
            wobble = dp(random.range(4f, 18f)),
            twinkle = random.range(0.15f, 0.5f),
            phase = random.nextFloat() * 2f * PI.toFloat(),
        )
    }

    private fun step(dt: Float) {
        seconds += dt
        val w = width.toFloat()
        val h = height.toFloat()
        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt
            val r = p.radius
            if (p.x < -r) p.x += w + 2 * r else if (p.x > w + r) p.x -= w + 2 * r
            if (p.y < -r) p.y += h + 2 * r else if (p.y > h + r) p.y -= h + 2 * r
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (p in particles) {
            val sway = sin(seconds * p.twinkle + p.phase)
            p.paint.alpha = (p.alpha * (TWINKLE_BASE + (1 - TWINKLE_BASE) * (0.5f + 0.5f * sway))).toInt()
            canvas.save()
            canvas.translate(p.x + sway * p.wobble, p.y)
            canvas.drawCircle(0f, 0f, p.radius, p.paint)
            canvas.restore()
        }
    }

    private fun lightColors(palette: Palette, accent: Int): IntArray {
        val second = if (palette.glow != 0) Contrast.withAlpha(palette.glow, 0xFF) else Contrast.blend(accent, palette.text, 0.5f)
        return if (palette.light) {
            intArrayOf(accent, Contrast.blend(accent, Color.WHITE, 0.4f), second)
        } else {
            intArrayOf(accent, palette.text, second)
        }
    }

    private fun tint(white: Int, color: Int): Int = Contrast.withAlpha(color, Color.alpha(white))

    private fun white(alpha: Int): Int = Contrast.withAlpha(Color.WHITE, alpha)

    private fun dp(value: Float): Float = value * density

    private fun Random.range(from: Float, to: Float): Float = from + nextFloat() * (to - from)

    private companion object {
        const val SEED = 7
        const val LIGHTS = 11
        const val SPARKS = 26
        const val LIGHT_MIN_DP = 50f
        const val LIGHT_MAX_DP = 150f
        const val LIGHT_ALPHA = 0x30
        const val LIGHT_ALPHA_ON_LIGHT = 0x40
        const val SPARK_MIN_DP = 1.2f
        const val SPARK_MAX_DP = 2.6f
        const val SPARK_HALO = 5f
        const val SPARK_CORE = 0.2f
        const val SPARK_ALPHA = 0xC0
        const val SPARK_RISE_DP = 9f
        const val SPEED_LIGHT_DP = 9f
        const val SPEED_SPARK_DP = 7f
        const val VERTICAL = 0.5f
        const val TWINKLE_BASE = 0.45f
        const val FRAME_NS = 30_000_000L
        const val MAX_STEP_NS = 100_000_000L
        const val HOLD_MS = 450L
    }
}
