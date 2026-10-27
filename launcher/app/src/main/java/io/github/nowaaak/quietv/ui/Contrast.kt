package io.github.nowaaak.quietv.ui

import kotlin.math.pow

object Contrast {

    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    fun blend(under: Int, over: Int, amount: Float): Int {
        fun mix(shift: Int): Int {
            val u = (under shr shift) and 0xFF
            val o = (over shr shift) and 0xFF
            return (u + (o - u) * amount).toInt().coerceIn(0, 255) shl shift
        }
        return (0xFF shl 24) or mix(16) or mix(8) or mix(0)
    }

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0xFFFFFF) or (alpha shl 24)

    fun scrimFor(samples: IntArray, texts: IntArray, scrim: Int, minimum: Float, target: Double = TARGET): Float {
        if (samples.isEmpty() || texts.isEmpty()) return minimum
        val needed = samples.map { sample ->
            var amount = 0f
            while (amount < 1f && texts.any { ratio(it, blend(sample, scrim, amount)) < target }) amount += STEP
            amount.coerceAtMost(1f)
        }.sorted()
        val percentile = needed[((needed.size - 1) * PERCENTILE).toInt()]
        return maxOf(minimum, percentile)
    }

    const val TARGET = 4.5
    private const val STEP = 0.05f
    private const val PERCENTILE = 0.9
}
