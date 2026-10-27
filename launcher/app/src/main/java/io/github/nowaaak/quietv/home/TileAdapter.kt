package io.github.nowaaak.quietv.home

import android.annotation.SuppressLint
import android.graphics.Outline
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.ui.themeColor

class TileAdapter(
    private val listener: Listener,
    private val dockSize: Int,
    private val tileWidth: Int,
    private val tileHeight: Int,
) : RecyclerView.Adapter<TileAdapter.Holder>() {

    interface Listener {
        fun onTileFocused(position: Int, card: View)
        fun onTileKey(position: Int, card: View, event: KeyEvent): Boolean
        fun onTileClick(position: Int)
        fun onTileMenu(position: Int, card: View)
    }

    private val items = mutableListOf<AppEntry>()

    val entries: List<AppEntry> get() = items

    @SuppressLint("NotifyDataSetChanged")
    fun submit(fresh: List<AppEntry>) {
        items.clear()
        items.addAll(fresh)
        notifyDataSetChanged()
    }

    fun move(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
    }

    fun entryAt(position: Int): AppEntry? = items.getOrNull(position)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = items[position]
        val dock = position < dockSize
        holder.name.text = entry.label
        holder.name.alpha = if (dock) 1f else 0f
        holder.name.translationY = if (dock) 0f else -6 * holder.density
        holder.name.setTextColor(if (dock) holder.dimText else holder.text)
        holder.card.scaleX = 1f
        holder.card.scaleY = 1f
        if (entry.banner != null) {
            holder.banner.setImageDrawable(entry.banner)
            holder.banner.visibility = View.VISIBLE
            holder.icon.visibility = View.GONE
            holder.label.visibility = View.GONE
        } else {
            holder.banner.setImageDrawable(null)
            holder.banner.visibility = View.GONE
            holder.icon.setImageDrawable(entry.icon)
            holder.icon.visibility = View.VISIBLE
            holder.label.text = entry.label
            holder.label.visibility = View.VISIBLE
        }
        holder.card.contentDescription = entry.label
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.card)
        val banner: ImageView = view.findViewById(R.id.banner)
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val name: TextView = view.findViewById(R.id.name)
        val density = view.resources.displayMetrics.density
        val text = view.context.themeColor(R.attr.qvText)
        val dimText = view.context.themeColor(R.attr.qvTextDim)

        init {
            card.layoutParams = (card.layoutParams as FrameLayout.LayoutParams).apply {
                width = tileWidth
                height = tileHeight
            }
            name.layoutParams = (name.layoutParams as FrameLayout.LayoutParams).apply {
                width = tileWidth
                topMargin = tileHeight + (NAME_GAP_DP * density).toInt()
            }
            val radius = density * CORNER_DP
            card.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            card.clipToOutline = true

            card.setOnClickListener {
                position()?.let { listener.onTileClick(it) }
            }
            card.setOnLongClickListener {
                position()?.let { listener.onTileMenu(it, card) }
                true
            }
            card.setOnKeyListener { v, _, event ->
                val pos = position() ?: return@setOnKeyListener false
                val handled = listener.onTileKey(pos, v, event)
                if (handled && event.action == KeyEvent.ACTION_UP) {
                    v.cancelLongPress()
                    v.isPressed = false
                }
                handled
            }
            card.setOnFocusChangeListener { v, focused ->
                val scale = if (focused) FOCUS_SCALE else 1f
                v.animate().scaleX(scale).scaleY(scale)
                    .setDuration(if (focused) 260 else 180)
                    .setInterpolator(OvershootInterpolator(if (focused) 1.6f else 0f))
                    .start()
                val pos = position()
                if (pos != null && pos < dockSize) {
                    name.setTextColor(if (focused) text else dimText)
                    name.animate().alpha(1f).translationY(if (focused) density * 4f else 0f).setDuration(220).start()
                } else {
                    name.animate().alpha(if (focused) 1f else 0f)
                        .translationY(if (focused) density * 4f else -density * 6f)
                        .setDuration(220).start()
                }
                if (focused && pos != null) listener.onTileFocused(pos, v)
            }
        }

        private fun position(): Int? = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
    }

    companion object {
        const val FOCUS_SCALE = 1.06f
        const val CORNER_DP = 14f
        const val NAME_GAP_DP = 9f
        const val NAME_HEIGHT_DP = 22f
        const val VERTICAL_PADDING_DP = 8f
    }
}
