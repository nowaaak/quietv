package io.github.nowaaak.quietv.search

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.home.AppCatalog
import io.github.nowaaak.quietv.home.AppEntry
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors

class SearchActivity : Activity() {

    private lateinit var palette: Palette
    private lateinit var results: LinearLayout
    private lateinit var empty: TextView
    private var apps: List<AppEntry> = emptyList()
    private var matches: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        val store = ConfigStore(this)
        val config = store.load()
        palette = Palette.of(config)
        ThemeColors.apply(this, palette)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        results = findViewById(R.id.results)
        empty = findViewById(R.id.empty)
        empty.setTextColor(palette.textMuted)
        apps = AppCatalog(this).load(config, store.tileOrder).filter { !it.isStockHome }.sortedBy { it.label.lowercase() }

        val query = findViewById<EditText>(R.id.query)
        query.backgroundTintList = ColorStateList.valueOf(palette.surface)
        query.setTextColor(palette.text)
        query.setHintTextColor(palette.textMuted)
        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = render(s?.toString().orEmpty())
        })
        query.setOnEditorActionListener { _, action, event ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (action != EditorInfo.IME_ACTION_SEARCH && !enter) return@setOnEditorActionListener false
            matches.firstOrNull()?.let { open(it) }
            true
        }
        render("")
        query.requestFocus()
    }

    private fun render(text: String) {
        val needle = text.trim()
        matches = if (needle.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(needle, ignoreCase = true) }
                .sortedBy { if (it.label.startsWith(needle, ignoreCase = true)) 0 else 1 }
        }
        results.removeAllViews()
        empty.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
        val content = ThemeColors.pillContent(palette)
        for (entry in matches) {
            val row = layoutInflater.inflate(R.layout.item_search, results, false)
            row.backgroundTintList = ThemeColors.rowBackground(palette)
            row.findViewById<ImageView>(R.id.icon).setImageDrawable(entry.icon)
            row.findViewById<TextView>(R.id.title).apply {
                this.text = entry.label
                setTextColor(content)
            }
            row.setOnClickListener { open(entry) }
            results.addView(row)
        }
    }

    private fun open(entry: AppEntry) {
        AppLauncher.launch(this, entry)
        finish()
    }
}
