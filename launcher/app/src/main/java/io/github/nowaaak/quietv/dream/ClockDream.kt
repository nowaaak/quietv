package io.github.nowaaak.quietv.dream

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import io.github.nowaaak.quietv.R
import kotlin.random.Random

class ClockDream : DreamService() {

    private val handler = Handler(Looper.getMainLooper())
    private var clock: View? = null
    private val drift = object : Runnable {
        override fun run() {
            move()
            handler.postDelayed(this, DRIFT_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(R.layout.dream_clock)
        clock = findViewById(R.id.dreamClock)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.postDelayed(drift, DRIFT_MS)
    }

    override fun onDreamingStopped() {
        handler.removeCallbacks(drift)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun move() {
        val view = clock ?: return
        val parent = view.parent as? View ?: return
        val roomX = parent.width - view.width
        val roomY = parent.height - view.height
        if (roomX <= 0 || roomY <= 0) return
        view.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
            view.translationX = Random.nextInt(roomX) - view.left.toFloat()
            view.translationY = Random.nextInt(roomY) - view.top.toFloat()
            view.animate().alpha(1f).setDuration(FADE_MS).start()
        }.start()
    }

    private companion object {
        const val DRIFT_MS = 60_000L
        const val FADE_MS = 800L
    }
}
