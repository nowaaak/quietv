package io.github.nowaaak.quietv.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): LauncherConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return LauncherConfig()
        return try {
            LauncherConfig.parse(raw)
        } catch (e: InvalidConfigException) {
            Log.w(TAG, "stored config is invalid, using defaults: ${e.message}")
            LauncherConfig()
        }
    }

    fun save(config: LauncherConfig) {
        prefs.edit().putString(KEY_CONFIG, config.toJson()).apply()
    }

    fun update(change: (LauncherConfig) -> LauncherConfig) {
        save(change(load()))
    }

    var tileOrder: List<String>
        get() = prefs.getString(KEY_ORDER, "").orEmpty().split(',').filter { it.isNotEmpty() }
        set(value) {
            prefs.edit().putString(KEY_ORDER, value.joinToString(",")).apply()
        }

    var lastInput: String
        get() = prefs.getString(KEY_LAST_INPUT, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_LAST_INPUT, value).apply()
        }

    var startupBoot: Int
        get() = prefs.getInt(KEY_STARTUP_BOOT, -1)
        set(value) {
            prefs.edit().putInt(KEY_STARTUP_BOOT, value).apply()
        }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        const val TAG = "ConfigStore"
        const val PREFS = "quietv"
        const val KEY_CONFIG = "config"
        const val KEY_ORDER = "order"
        const val KEY_LAST_INPUT = "lastInput"
        const val KEY_STARTUP_BOOT = "startupBoot"
    }
}
