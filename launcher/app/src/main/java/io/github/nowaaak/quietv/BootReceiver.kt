package io.github.nowaaak.quietv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nowaaak.quietv.companion.CompanionReceiverService
import io.github.nowaaak.quietv.config.ConfigStore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CompanionReceiverService.sync(context, ConfigStore(context).load())
        }
    }
}
