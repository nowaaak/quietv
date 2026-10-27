package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.ThemeMode
import io.github.nowaaak.quietv.firetv.HomeRedirectService
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.home.AppCatalog
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.ui.ThemeColors

class SettingsActivity : Activity() {

    private lateinit var store: ConfigStore
    private lateinit var rows: LinearLayout
    private var theme = ThemeMode.DARK

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ConfigStore(this)
        theme = store.load().theme
        ThemeColors.apply(this, theme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        rows = findViewById(R.id.rows)
    }

    override fun onResume() {
        super.onResume()
        render(focus = 0)
    }

    private fun render(focus: Int) {
        rows.removeAllViews()
        val config = store.load()
        val catalog = AppCatalog(this)

        section(R.string.settings_appearance)
        row(getString(R.string.settings_theme), themeName(config.theme)) {
            val next = if (config.theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            store.update { it.copy(theme = next, accent = ThemeColors.accentForTheme(it.accent, it.theme, next)) }
            recreate()
        }
        row(getString(R.string.settings_accent), LauncherConfig.formatColor(config.accent), swatch = config.accent) {
            store.update { it.copy(accent = ThemeColors.nextAccent(it.theme, it.accent)) }
        }
        row(getString(R.string.settings_columns), config.columns.toString()) {
            val next = if (config.columns >= MAX_COLUMNS_ON_TV) MIN_COLUMNS_ON_TV else config.columns + 1
            store.update { it.copy(columns = next) }
        }

        section(R.string.settings_apps)
        row(getString(R.string.settings_reset_order), null) {
            store.tileOrder = emptyList()
            Toast.makeText(this, R.string.settings_order_reset, Toast.LENGTH_SHORT).show()
        }
        for (pkg in config.hidden) {
            row(getString(R.string.settings_show_app, catalog.label(pkg)), getString(R.string.settings_hidden)) {
                store.update { it.copy(hidden = it.hidden - pkg) }
            }
        }

        if (Platform.isFireOs(this) || config.fireTv.stockHomePackage.isNotEmpty()) {
            section(R.string.settings_fire_tv)
            val on = Platform.redirectServiceEnabled(this)
            row(getString(R.string.settings_home_redirect), getString(if (on) R.string.on else R.string.off)) {
                AppLauncher.start(this, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        section(R.string.settings_system)
        row(getString(R.string.settings_tv_settings), null) {
            HomeRedirectService.passStockHomeOnce()
            AppLauncher.start(this, Intent(Settings.ACTION_SETTINGS))
        }
        row(getString(R.string.settings_about), getString(R.string.settings_version, versionName()), focusable = false) {}

        val focusable = (0 until rows.childCount).map { rows.getChildAt(it) }.filter { it.isFocusable }
        focusable.getOrNull(focus.coerceIn(0, (focusable.size - 1).coerceAtLeast(0)))?.requestFocus()
    }

    private fun section(title: Int) {
        val view = layoutInflater.inflate(R.layout.item_settings_section, rows, false) as TextView
        view.setText(title)
        rows.addView(view)
    }

    private fun row(
        title: String,
        value: String?,
        swatch: Int? = null,
        focusable: Boolean = true,
        action: () -> Unit,
    ) {
        val view = layoutInflater.inflate(R.layout.item_setting, rows, false)
        view.findViewById<TextView>(R.id.title).text = title
        val valueView = view.findViewById<TextView>(R.id.value)
        if (value == null) valueView.visibility = View.GONE else valueView.text = value
        val swatchView = view.findViewById<View>(R.id.swatch)
        if (swatch == null) {
            swatchView.visibility = View.GONE
        } else {
            swatchView.backgroundTintList = ColorStateList.valueOf(swatch)
        }
        view.isFocusable = focusable
        view.isClickable = focusable
        if (focusable) {
            view.setOnClickListener {
                val index = focusableIndex(view)
                action()
                if (!isFinishing && !isDestroyed) render(index)
            }
        }
        rows.addView(view)
    }

    private fun focusableIndex(view: View): Int =
        (0 until rows.childCount).map { rows.getChildAt(it) }.filter { it.isFocusable }.indexOf(view)

    private fun themeName(mode: ThemeMode): String =
        getString(if (mode == ThemeMode.LIGHT) R.string.theme_light else R.string.theme_dark)

    private fun versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    private companion object {
        const val MIN_COLUMNS_ON_TV = 4
        const val MAX_COLUMNS_ON_TV = 6
    }
}
