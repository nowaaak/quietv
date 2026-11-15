package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import io.github.nowaaak.quietv.config.StartupConfig
import io.github.nowaaak.quietv.config.WallpaperConfig
import io.github.nowaaak.quietv.firetv.HomeRedirectService
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.home.AppCatalog
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.home.Shortcuts
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.wallpaper.Scenes
import io.github.nowaaak.quietv.wallpaper.WallpaperStore

class SettingsActivity : Activity() {

    private lateinit var store: ConfigStore
    private lateinit var rows: LinearLayout
    private lateinit var palette: Palette
    private var group: LinearLayout? = null

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
        group = null
        val config = store.load()
        val catalog = AppCatalog(this)

        section(R.string.settings_appearance)
        row(getString(R.string.settings_theme), themeName(config), rerender = false) { anchor ->
            ThemePicker(this, palette, config) { next ->
                store.update {
                    it.copy(
                        theme = next,
                        colors = emptyMap(),
                        accent = ThemeColors.accentForTheme(it.accent, palette.light, next.light),
                    )
                }
                recreate()
            }.show(anchor)
        }
        row(
            getString(R.string.settings_accent),
            LauncherConfig.formatColor(config.accent),
            swatch = config.accent,
            rerender = false,
        ) { anchor ->
            val index = focusableIndex(anchor)
            AccentPicker(this, palette, config) { color ->
                store.update { it.copy(accent = color) }
                if (!isFinishing && !isDestroyed) render(index)
            }.show(anchor)
        }
        row(getString(R.string.settings_tile_shape), tileShapeName(config.tileRadius)) {
            val next = TILE_RADII.firstOrNull { it > config.tileRadius } ?: TILE_RADII.first()
            store.update { it.copy(tileRadius = next) }
        }
        val wallpaper = config.wallpaper
        val choices = backgroundChoices()
        val current = currentBackground(wallpaper, choices)
        row(getString(R.string.settings_background), backgroundName(current)) {
            val next = choices[(choices.indexOf(current) + 1) % choices.size]
            store.update {
                it.copy(
                    wallpaper = it.wallpaper.copy(
                        enabled = next != BACKGROUND_COLOR,
                        scene = if (next == BACKGROUND_IMAGE || next == BACKGROUND_COLOR) "" else next,
                    ),
                )
            }
        }
        if (current != BACKGROUND_COLOR) {
            row(getString(R.string.settings_wallpaper_dim), getString(R.string.percent, wallpaper.dim)) {
                val next = WALLPAPER_DIMS.firstOrNull { it > wallpaper.dim } ?: WALLPAPER_DIMS.first()
                store.update { it.copy(wallpaper = it.wallpaper.copy(dim = next)) }
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

        section(R.string.settings_remote)
        if (config.shortcuts.isEmpty()) {
            row(getString(R.string.settings_shortcut_hint), null, focusable = false) {}
        }
        for (key in LauncherConfig.SHORTCUT_KEYS.filter { it in config.shortcuts }) {
            val pkg = config.shortcuts.getValue(key)
            val title = getString(R.string.settings_shortcut, Shortcuts.label(this, key), catalog.label(pkg))
            row(title, getString(R.string.settings_remove)) {
                store.update { it.copy(shortcuts = it.shortcuts - key) }
            }
        }

        if (Platform.isFireOs(this) || config.homeRedirect.stockHomePackage.isNotEmpty()) {
            section(R.string.settings_home_button)
            val on = Platform.redirectServiceEnabled(this)
            row(getString(R.string.settings_home_redirect), getString(if (on) R.string.on else R.string.off)) {
                AppLauncher.start(this, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        section(R.string.settings_system)
        val startup = config.startup
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_TV) || startup != StartupConfig()) {
            row(getString(R.string.settings_startup), startupName(startup, catalog)) {
                val next = if (startup == StartupConfig()) StartupConfig(input = StartupConfig.LAST_INPUT) else StartupConfig()
                store.update { it.copy(startup = next) }
            }
        }
        row(getString(R.string.settings_tv_settings), null) {
            HomeRedirectService.passStockHomeOnce()
            AppLauncher.start(this, Intent(Settings.ACTION_SETTINGS))
        }
        row(getString(R.string.settings_about), getString(R.string.settings_version, versionName()), focusable = false) {}

        val focusable = focusableRows()
        focusable.getOrNull(focus.coerceIn(0, (focusable.size - 1).coerceAtLeast(0)))?.requestFocus()
    }

    private fun section(title: Int) {
        val view = layoutInflater.inflate(R.layout.item_settings_section, rows, false) as TextView
        view.setText(title)
        view.setTextColor(palette.textMuted)
        rows.addView(view)
        group = if (palette.glass) glassGroup() else null
    }

    private fun row(
        title: String,
        value: String?,
        swatch: Int? = null,
        focusable: Boolean = true,
        rerender: Boolean = true,
        action: (View) -> Unit,
    ) {
        val parent = group ?: rows
        val view = layoutInflater.inflate(R.layout.item_setting, parent, false)
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
                action(view)
                if (rerender && !isFinishing && !isDestroyed) render(index)
            }
        }
        parent.addView(view)
    }

    private fun glassGroup(): LinearLayout {
        val density = resources.displayMetrics.density
        val radius = GROUP_RADIUS_DP * density
        val padding = (GROUP_PADDING_DP * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = ThemeColors.edged(palette, radius, density.toInt().coerceAtLeast(1))
            rows.addView(this)
        }
    }

    private fun focusableRows(): List<View> = (0 until rows.childCount).map { rows.getChildAt(it) }.flatMap { child ->
        if (child is LinearLayout && !child.isFocusable) {
            (0 until child.childCount).map { child.getChildAt(it) }
        } else {
            listOf(child)
        }
    }.filter { it.isFocusable }

    private fun focusableIndex(view: View): Int = focusableRows().indexOf(view)

    private fun themeName(config: LauncherConfig): String {
        val name = getString(ThemePicker.nameRes(config.theme))
        return if (config.colors.isEmpty()) name else getString(R.string.theme_custom, name)
    }

    private fun startupName(startup: StartupConfig, catalog: AppCatalog): String = when {
        startup.app.isNotEmpty() -> catalog.label(startup.app)
        startup.input == StartupConfig.LAST_INPUT -> getString(R.string.startup_last_input)
        startup.input.isNotEmpty() -> getString(R.string.startup_input)
        else -> getString(R.string.startup_home)
    }

    private fun tileShapeName(radius: Int): String = when (radius) {
        0 -> getString(R.string.tile_square)
        in 1..8 -> getString(R.string.tile_soft)
        in 9..16 -> getString(R.string.tile_rounded)
        else -> getString(R.string.tile_round)
    }

    private fun backgroundChoices(): List<String> {
        val image = if (WallpaperStore(this).exists()) listOf(BACKGROUND_IMAGE) else emptyList()
        return listOf(BACKGROUND_COLOR) + WallpaperConfig.SCENES + image
    }

    private fun currentBackground(wallpaper: WallpaperConfig, choices: List<String>): String {
        val current = when {
            !wallpaper.enabled -> BACKGROUND_COLOR
            wallpaper.scene.isNotEmpty() -> wallpaper.scene
            else -> BACKGROUND_IMAGE
        }
        return if (current in choices) current else BACKGROUND_COLOR
    }

    private fun backgroundName(choice: String): String = when (choice) {
        BACKGROUND_COLOR -> getString(R.string.background_color)
        BACKGROUND_IMAGE -> getString(R.string.background_image)
        else -> Scenes.label(choice)?.let { getString(it) } ?: choice
    }

    private fun versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    private companion object {
        const val BACKGROUND_COLOR = "color"
        const val BACKGROUND_IMAGE = "image"
        const val MIN_COLUMNS_ON_TV = 4
        const val MAX_COLUMNS_ON_TV = 6
        val TILE_RADII = intArrayOf(0, 6, 14, 22)
        val WALLPAPER_DIMS = intArrayOf(0, 15, 30, 45, 60)
        const val GROUP_RADIUS_DP = 18f
        const val GROUP_PADDING_DP = 5f
    }
}
