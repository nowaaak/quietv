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
import io.github.nowaaak.quietv.config.ThemePreset
import io.github.nowaaak.quietv.firetv.HomeRedirectService
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.home.AppCatalog
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.wallpaper.WallpaperStore

class SettingsActivity : Activity() {

    private lateinit var store: ConfigStore
    private lateinit var rows: LinearLayout
    private lateinit var palette: Palette

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ConfigStore(this)
        palette = Palette.of(store.load())
        ThemeColors.apply(this, palette)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        rows = findViewById(R.id.rows)
        findViewById<TextView>(R.id.title).setTextColor(palette.text)
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
        row(getString(R.string.settings_theme), themeName(config)) {
            val next = ThemePreset.entries[(config.theme.ordinal + 1) % ThemePreset.entries.size]
            store.update {
                it.copy(
                    theme = next,
                    colors = emptyMap(),
                    accent = ThemeColors.accentForTheme(it.accent, palette.light, next.light),
                )
            }
            recreate()
        }
        row(getString(R.string.settings_accent), LauncherConfig.formatColor(config.accent), swatch = config.accent) {
            store.update { it.copy(accent = ThemeColors.nextAccent(palette.light, it.accent)) }
        }
        row(getString(R.string.settings_tile_shape), tileShapeName(config.tileRadius)) {
            val next = TILE_RADII.firstOrNull { it > config.tileRadius } ?: TILE_RADII.first()
            store.update { it.copy(tileRadius = next) }
        }
        if (WallpaperStore(this).exists()) {
            val wallpaper = config.wallpaper
            row(getString(R.string.settings_wallpaper), getString(if (wallpaper.enabled) R.string.on else R.string.off)) {
                store.update { it.copy(wallpaper = it.wallpaper.copy(enabled = !it.wallpaper.enabled)) }
            }
            if (wallpaper.enabled) {
                row(getString(R.string.settings_wallpaper_dim), getString(R.string.percent, wallpaper.dim)) {
                    val next = WALLPAPER_DIMS.firstOrNull { it > wallpaper.dim } ?: WALLPAPER_DIMS.first()
                    store.update { it.copy(wallpaper = it.wallpaper.copy(dim = next)) }
                }
            }
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
        view.setTextColor(palette.textMuted)
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
        view.backgroundTintList = ThemeColors.rowBackground(palette)
        view.findViewById<TextView>(R.id.title).apply {
            text = title
            setTextColor(ThemeColors.pillContent(palette))
        }
        val valueView = view.findViewById<TextView>(R.id.value)
        valueView.setTextColor(ThemeColors.pillContentDim(palette))
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

    private fun themeName(config: LauncherConfig): String {
        val name = getString(
            when (config.theme) {
                ThemePreset.GRAPHITE -> R.string.theme_graphite
                ThemePreset.PAPER -> R.string.theme_paper
                ThemePreset.MIDNIGHT -> R.string.theme_midnight
                ThemePreset.SLATE -> R.string.theme_slate
                ThemePreset.FOREST -> R.string.theme_forest
                ThemePreset.SAND -> R.string.theme_sand
            },
        )
        return if (config.colors.isEmpty()) name else getString(R.string.theme_custom, name)
    }

    private fun tileShapeName(radius: Int): String = when (radius) {
        0 -> getString(R.string.tile_square)
        in 1..8 -> getString(R.string.tile_soft)
        in 9..16 -> getString(R.string.tile_rounded)
        else -> getString(R.string.tile_round)
    }

    private fun versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    private companion object {
        const val MIN_COLUMNS_ON_TV = 4
        const val MAX_COLUMNS_ON_TV = 6
        val TILE_RADII = intArrayOf(0, 6, 14, 22)
        val WALLPAPER_DIMS = intArrayOf(0, 15, 30, 45, 60)
    }
}
