package io.github.nowaaak.quietv.inputs

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

data class InputEntry(
    val id: String,
    val portId: String,
    val label: String,
    val hdmi: Boolean,
    val state: Int,
    val hasDevice: Boolean,
)

object InputCatalog {

    fun passthrough(context: Context): List<InputEntry> {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager? ?: return emptyList()
        val passthrough = tim.tvInputList.filter { it.isPassthroughInput }
        val ids = passthrough.map { it.id }.toSet()
        val devices = passthrough.filter { it.parentId != null && it.parentId in ids }.groupBy { it.parentId }
        val ports = passthrough.filter { it.parentId == null || it.parentId !in ids }.sortedWith(
            compareBy<TvInputInfo> { it.type != TvInputInfo.TYPE_HDMI }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.loadLabel(context).toString() },
        )
        return ports.map { port ->
            val hdmi = port.type == TvInputInfo.TYPE_HDMI
            var label = port.loadLabel(context)?.toString() ?: port.id
            if (hdmi && !label.uppercase().contains("HDMI")) label = "HDMI $label"
            val device = devices[port.id]?.firstOrNull()
            device?.loadLabel(context)?.toString()?.takeIf { it.isNotBlank() }?.let { label = "$label · $it" }
            val id = device?.id ?: port.id
            InputEntry(id, port.id, label, hdmi, tim.getInputState(id), device != null)
        }
    }
}
