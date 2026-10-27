package io.github.nowaaak.quietv.firetv

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.github.nowaaak.quietv.config.ConfigStore

object RedirectRebind {

    private const val TAG = "RedirectRebind"
    private const val CHECK_DELAY_MS = 5000L
    private const val REBIND_DELAY_MS = 1000L

    fun restore(context: Context) {
        val app = context.applicationContext
        if (!ConfigStore(app).redirectWanted || Platform.redirectServiceEnabled(app)) return
        val resolver = app.contentResolver
        val ours = ComponentName(app, HomeRedirectService::class.java).flattenToString()
        val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val services = current.orEmpty().split(':').filter { it.isNotEmpty() && it != "null" } + ours
        try {
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services.joinToString(":"))
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            Log.i(TAG, "redirect service was removed, probably by a force stop, added it back")
        } catch (e: SecurityException) {
            Log.w(TAG, "cannot add the redirect service back: ${e.message}")
        }
    }

    fun schedule(context: Context, always: Boolean, done: () -> Unit) {
        val app = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (!Platform.redirectServiceEnabled(app) || (HomeRedirectService.connected && !always)) {
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
                    Log.i(TAG, "rebound the redirect service")
                } catch (e: SecurityException) {
                    Log.w(TAG, "cannot restore the accessibility services: ${e.message}")
                }
                done()
            }, REBIND_DELAY_MS)
        }, CHECK_DELAY_MS)
    }
}
