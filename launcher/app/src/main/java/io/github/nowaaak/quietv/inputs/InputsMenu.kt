package io.github.nowaaak.quietv.inputs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.ui.DropdownMenu
import io.github.nowaaak.quietv.ui.Palette

class InputsMenu(
    private val activity: Activity,
    private val liveTvPackage: String,
    private val palette: Palette,
    private val accent: Int,
    private val store: ConfigStore,
) {

    fun show(anchor: View, root: ViewGroup) {
        val menu = DropdownMenu(activity, root, palette, accent)
        val tim = activity.getSystemService(Activity.TV_INPUT_SERVICE) as TvInputManager?
        var sawNonHdmi = false
        var tunerId: String? = null
        var rowCount = 0
        var lastIndex = 0
        val last = store.lastInput

        val all = tim?.tvInputList.orEmpty()
        for (info in all) {
            val system = info.serviceInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val currentTuner = tunerId
            if (info.type == TvInputInfo.TYPE_TUNER && system && (currentTuner == null || info.id < currentTuner)) {
                tunerId = info.id
            }
        }
        for (input in InputCatalog.passthrough(activity)) {
            if (!input.hdmi && !menu.isEmpty && !sawNonHdmi) menu.addSeparator()
            if (!input.hdmi) sawNonHdmi = true
            if (input.id == last || input.portId == last) lastIndex = rowCount
            rowCount++
            menu.addRow(
                icon = if (input.hdmi) R.drawable.ic_input else R.drawable.ic_av,
                title = input.label,
                status = statusText(input.state, input.hasDevice),
                active = input.state == TvInputManager.INPUT_STATE_CONNECTED,
            ) {
                store.lastInput = input.id
                openInput(activity, input.id, liveTvPackage)
            }
        }

        val tuner = tunerId
        val liveTv = when {
            liveTvPackage.isEmpty() -> null
            activity.packageManager.getLeanbackLaunchIntentForPackage(liveTvPackage) == null -> null
            tuner != null -> inputIntent(tuner).setPackage(liveTvPackage)
            else -> activity.packageManager.getLeanbackLaunchIntentForPackage(liveTvPackage)
        }
        if (liveTv != null) {
            if (!menu.isEmpty) menu.addSeparator()
            menu.addRow(R.drawable.ic_livetv, activity.getString(R.string.live_tv)) { open(liveTv) }
        }

        if (menu.isEmpty) {
            Toast.makeText(activity, R.string.no_inputs, Toast.LENGTH_SHORT).show()
            return
        }
        menu.show(anchor, Gravity.END, focusIndex = lastIndex)
    }

    private fun statusText(state: Int, device: Boolean): String? = when {
        state == TvInputManager.INPUT_STATE_CONNECTED -> activity.getString(R.string.connected)
        state == TvInputManager.INPUT_STATE_DISCONNECTED -> activity.getString(R.string.not_connected)
        state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY && device -> activity.getString(R.string.standby)
        else -> null
    }

    private fun open(intent: Intent) = AppLauncher.start(activity, intent)

    companion object {
        fun openInput(context: Context, inputId: String, liveTvPackage: String) {
            val intent = inputIntent(inputId)
            if (liveTvPackage.isNotEmpty()) {
                val targeted = Intent(intent).setPackage(liveTvPackage)
                if (context.packageManager.resolveActivity(targeted, 0) != null) {
                    AppLauncher.start(context, targeted)
                    return
                }
            }
            AppLauncher.start(context, intent)
        }

        fun inputIntent(inputId: String): Intent =
            Intent(Intent.ACTION_VIEW).setDataAndType(
                TvContract.buildChannelUriForPassthroughInput(inputId),
                "vnd.android.cursor.item/channel",
            )
    }
}
