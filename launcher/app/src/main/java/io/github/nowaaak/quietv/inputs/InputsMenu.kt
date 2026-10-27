package io.github.nowaaak.quietv.inputs

import android.app.Activity
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
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.ui.DropdownMenu

class InputsMenu(private val activity: Activity, private val liveTvPackage: String, private val accent: Int) {

    fun show(anchor: View, root: ViewGroup) {
        val menu = DropdownMenu(activity, root, accent)
        val tim = activity.getSystemService(Activity.TV_INPUT_SERVICE) as TvInputManager?
        var sawNonHdmi = false
        var tunerId: String? = null

        val inputs = tim?.tvInputList.orEmpty().sortedWith(
            compareBy<TvInputInfo> { it.type != TvInputInfo.TYPE_HDMI }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.loadLabel(activity).toString() },
        )
        for (info in inputs) {
            val system = info.serviceInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val currentTuner = tunerId
            if (info.type == TvInputInfo.TYPE_TUNER && system && (currentTuner == null || info.id < currentTuner)) {
                tunerId = info.id
            }
            if (tim == null || !info.isPassthroughInput) continue
            val hdmi = info.type == TvInputInfo.TYPE_HDMI
            var label = info.loadLabel(activity)?.toString() ?: info.id
            if (hdmi && !label.uppercase().contains("HDMI")) label = "HDMI $label"
            if (!hdmi && !menu.isEmpty && !sawNonHdmi) menu.addSeparator()
            if (!hdmi) sawNonHdmi = true
            val id = info.id
            val state = tim.getInputState(id)
            menu.addRow(
                icon = if (hdmi) R.drawable.ic_input else R.drawable.ic_av,
                title = label,
                status = statusText(state),
                active = state == TvInputManager.INPUT_STATE_CONNECTED,
            ) { open(channelIntent(id)) }
        }

        val tuner = tunerId
        val liveTv = when {
            liveTvPackage.isEmpty() -> null
            activity.packageManager.getLeanbackLaunchIntentForPackage(liveTvPackage) == null -> null
            tuner != null -> channelIntent(tuner).setPackage(liveTvPackage)
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
        menu.showBelow(anchor, Gravity.END)
    }

    private fun statusText(state: Int): String? = when (state) {
        TvInputManager.INPUT_STATE_CONNECTED -> activity.getString(R.string.connected)
        TvInputManager.INPUT_STATE_DISCONNECTED -> activity.getString(R.string.not_connected)
        else -> null
    }

    private fun channelIntent(inputId: String): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(
            TvContract.buildChannelUriForPassthroughInput(inputId),
            "vnd.android.cursor.item/channel",
        )

    private fun open(intent: Intent) = AppLauncher.start(activity, intent)
}
