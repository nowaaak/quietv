package io.github.nowaaak.quietv.firetv

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

object RedirectRebind {

    private const val TAG = "RedirectRebind"
    private const val CHECK_DELAY_MS = 5000L
    private const val REBIND_DELAY_MS = 1000L

    fun schedule(context: Context, done: () -> Unit) {
        val app = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (!Platform.redirectServiceEnabled(app) || Platform.redirectServiceRunning(app)) {
                done()
                return@postDelayed
            }
            val resolver = app.contentResolver
            val services = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val ours = ComponentName(app, HomeRedirectService::class.java)
            val others = services.split(':').filter { it.isNotEmpty() && ComponentName.unflattenFromString(it) != ours }
            try {
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, others.joinToString(":"))
            } catch (e: SecurityException) {
                Log.w(TAG, "cannot rebind the redirect service: ${e.message}")
                done()
                return@postDelayed
            }
            handler.postDelayed({
                try {
                    Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services)
                    Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
                    Log.i(TAG, "redirect service was not running after boot, rebound it")
                } catch (e: SecurityException) {
                    Log.w(TAG, "cannot restore the accessibility services: ${e.message}")
                }
                done()
            }, REBIND_DELAY_MS)
        }, CHECK_DELAY_MS)
    }
}
