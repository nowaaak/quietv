package io.github.nowaaak.quietv.firetv

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
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

    override fun onServiceConnected() {
        connected = true
        val configStore = ConfigStore(this)
        configStore.redirectWanted = true
        store = configStore
        configStore.register(this)
        refresh()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        store?.redirectWanted = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
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
            if (!cls.contains(classHint) || allowStock || grace.active(SystemClock.uptimeMillis())) return
            if (SystemClock.uptimeMillis() < passStockUntil) {
                passStockUntil = 0
                return
            }
            startLauncher()
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
