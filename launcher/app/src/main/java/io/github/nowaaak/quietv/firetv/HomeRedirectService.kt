package io.github.nowaaak.quietv.firetv

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import io.github.nowaaak.quietv.MainActivity
import io.github.nowaaak.quietv.config.ConfigStore

class HomeRedirectService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var store: ConfigStore? = null

    @Volatile
    private var stockHome = ""

    @Volatile
    private var classHint = ""

    @Volatile
    private var redirect = false

    private val grace = AssistantGrace()
    private val handler = Handler(Looper.getMainLooper())
    private val pendingRedirect = Runnable { startLauncher() }
    private var settle = false
    private var otherStockWindowAt = 0L

    override fun onServiceConnected() {
        connected = true
        val configStore = ConfigStore(this)
        configStore.redirectWanted = true
        store = configStore
        configStore.register(this)
        settle = !Platform.isFireOs(this)
        refresh()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        store?.redirectWanted = Platform.redirectServiceEnabled(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        handler.removeCallbacks(pendingRedirect)
        store?.unregister(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        refresh()
    }

    private fun refresh() {
        val config = store?.load() ?: return
        stockHome = Platform.stockHomePackage(this, config)
        classHint = config.homeRedirect.classHint
        redirect = config.homeRedirect.enabled
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val stock = stockHome
        if (!redirect || stock.isEmpty()) return
        grace.windowShown(pkg, SystemClock.uptimeMillis())

        if (pkg == stock) {
            val cls = event.className?.toString() ?: return
            val now = SystemClock.uptimeMillis()
            if (!cls.contains(classHint)) {
                if (settle && !cls.startsWith("android.")) {
                    otherStockWindowAt = now
                    handler.removeCallbacks(pendingRedirect)
                }
                return
            }
            if (allowStock || grace.active(now)) return
            if (now < passStockUntil) {
                passStockUntil = 0
                return
            }
            if (!settle) {
                startLauncher()
            } else if (now - otherStockWindowAt >= OTHER_WINDOW_MS) {
                handler.removeCallbacks(pendingRedirect)
                handler.postDelayed(pendingRedirect, SETTLE_MS)
            }
        } else if (pkg != "android" && !pkg.startsWith("com.android.") && !pkg.startsWith(QUICK_SETTINGS)) {
            allowStock = false
        }
    }

    private fun startLauncher() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setComponent(ComponentName(this, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val PASS_THROUGH_MS = 3000L
        private const val SETTLE_MS = 150L
        private const val OTHER_WINDOW_MS = 500L
        private const val QUICK_SETTINGS = "com.amazon.tv.quicksettings"

        @Volatile
        var connected = false
            private set

        @Volatile
        private var allowStock = false

        @Volatile
        private var passStockUntil = 0L

        fun allowStockHome() {
            allowStock = true
        }

        fun passStockHomeOnce() {
            passStockUntil = SystemClock.uptimeMillis() + PASS_THROUGH_MS
        }
    }
}
