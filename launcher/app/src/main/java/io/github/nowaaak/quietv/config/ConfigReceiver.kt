package io.github.nowaaak.quietv.config

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import io.github.nowaaak.quietv.firetv.Platform

class ConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = ConfigStore(context)
        when (intent.action) {
            ACTION_GET -> {
                val json = store.load().toJson().toByteArray(Charsets.UTF_8)
                setResult(Activity.RESULT_OK, Base64.encodeToString(json, Base64.NO_WRAP), null)
            }
            ACTION_SET -> {
                val encoded = intent.getStringExtra(EXTRA_CONFIG)
                if (encoded == null) {
                    setResult(Activity.RESULT_CANCELED, "missing extra '$EXTRA_CONFIG'", null)
                    return
                }
                try {
                    val json = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
                    store.save(LauncherConfig.parse(json))
                    Platform.syncHomeEntry(context)
                    setResult(Activity.RESULT_OK, null, null)
                } catch (e: IllegalArgumentException) {
                    setResult(Activity.RESULT_CANCELED, "config is not valid base64", null)
                } catch (e: InvalidConfigException) {
                    setResult(Activity.RESULT_CANCELED, e.message, null)
                }
            }
        }
    }

    companion object {
        const val ACTION_SET = "io.github.nowaaak.quietv.action.SET_CONFIG"
        const val ACTION_GET = "io.github.nowaaak.quietv.action.GET_CONFIG"
        const val EXTRA_CONFIG = "config"
    }
}
