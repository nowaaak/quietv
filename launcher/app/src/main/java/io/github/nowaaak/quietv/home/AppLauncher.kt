package io.github.nowaaak.quietv.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.firetv.HomeRedirectService

object AppLauncher {

    fun launch(context: Context, entry: AppEntry) {
        val intent = if (entry.isStockHome) {
            HomeRedirectService.allowStockHome()
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(entry.pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(entry.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        start(context, intent, context.getString(R.string.cant_open, entry.label))
    }

    fun openDetails(context: Context, pkg: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, intent, context.getString(R.string.not_available))
    }

    fun start(context: Context, intent: Intent, failure: String = context.getString(R.string.not_available)) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
        }
    }
}
