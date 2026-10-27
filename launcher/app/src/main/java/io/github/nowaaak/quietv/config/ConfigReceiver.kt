package io.github.nowaaak.quietv.config

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.wallpaper.WallpaperStore

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
            ACTION_WALLPAPER_CHUNK -> {
                val data = intent.getStringExtra(EXTRA_DATA)
                if (data == null || !intent.hasExtra(EXTRA_OFFSET)) {
                    setResult(Activity.RESULT_CANCELED, "missing extra '$EXTRA_DATA' or '$EXTRA_OFFSET'", null)
                    return
                }
                val bytes = try {
                    Base64.decode(data, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                    setResult(Activity.RESULT_CANCELED, "chunk is not valid base64", null)
                    return
                }
                val error = WallpaperStore(context).writeChunk(
                    offset = intent.getLongExtra(EXTRA_OFFSET, -1L),
                    data = bytes,
                    last = intent.getBooleanExtra(EXTRA_LAST, false),
                    sha256 = intent.getStringExtra(EXTRA_SHA256),
                )
                setResult(if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED, error, null)
            }
            ACTION_CLEAR_WALLPAPER -> {
                WallpaperStore(context).clear()
                setResult(Activity.RESULT_OK, null, null)
            }
        }
    }

    companion object {
        const val ACTION_SET = "io.github.nowaaak.quietv.action.SET_CONFIG"
        const val ACTION_GET = "io.github.nowaaak.quietv.action.GET_CONFIG"
        const val ACTION_WALLPAPER_CHUNK = "io.github.nowaaak.quietv.action.WALLPAPER_CHUNK"
        const val ACTION_CLEAR_WALLPAPER = "io.github.nowaaak.quietv.action.CLEAR_WALLPAPER"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_DATA = "data"
        const val EXTRA_OFFSET = "offset"
        const val EXTRA_LAST = "last"
        const val EXTRA_SHA256 = "sha256"
    }
}
