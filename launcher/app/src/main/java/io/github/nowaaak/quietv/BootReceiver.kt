package io.github.nowaaak.quietv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nowaaak.quietv.companion.CompanionReceiverService
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.firetv.RedirectRebind

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CompanionReceiverService.sync(context, ConfigStore(context).load())
            RedirectRebind.restore(context)
            if (Platform.redirectServiceEnabled(context)) {
                val pending = goAsync()
                val updated = intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
                RedirectRebind.schedule(context, always = updated) { pending.finish() }
            }
        }
    }
}
