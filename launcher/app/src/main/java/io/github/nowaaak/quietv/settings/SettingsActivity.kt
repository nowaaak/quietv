package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
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
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.ui.ThemePreview
import io.github.nowaaak.quietv.wallpaper.Scenes
import io.github.nowaaak.quietv.wallpaper.WallpaperStore

/**
 * Categories on the left, the settings of the focused one on the right. Moving through the
 * categories shows their settings right away. Right or OK goes into them, Left or Back comes back.
 */
class SettingsActivity : Activity() {

    private enum class Category(val title: Int) {
        APPEARANCE(R.string.settings_appearance),
        APPS(R.string.settings_apps),
        REMOTE(R.string.settings_remote),
        HOME_BUTTON(R.string.settings_home_button),
        SYSTEM(R.string.settings_system),
    }

    private lateinit var store: ConfigStore
    private lateinit var palette: Palette
    private lateinit var categoryList: LinearLayout
    private lateinit var rows: LinearLayout
    private val categoryViews = LinkedHashMap<Category, View>()
    private var category = Category.APPEARANCE
    private var group: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ConfigStore(this)
        palette = Palette.of(store.load())
        ThemeColors.apply(this, palette)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        categoryList = findViewById(R.id.categories)
        rows = findViewById(R.id.rows)
        findViewById<TextView>(R.id.title).setTextColor(palette.text)
        findViewById<TextView>(R.id.subtitle).apply {
            text = getString(R.string.settings_version, versionName())
            setTextColor(palette.textMuted)
        }
    }

    override fun onResume() {
        super.onResume()
        buildCategories()
        render(focus = NO_FOCUS)
        categoryViews[category]?.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && inContent(currentFocus)) {
            categoryViews[category]?.requestFocus()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (inContent(currentFocus)) {
            categoryViews[category]?.requestFocus()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun buildCategories() {
        categoryList.removeAllViews()
        categoryViews.clear()
        val config = store.load()
        val available = Category.entries.filter {
            it != Category.HOME_BUTTON || Platform.isFireOs(this) || config.homeRedirect.stockHomePackage.isNotEmpty()
        }
        if (category !in available) category = Category.APPEARANCE
        val focusedSelectedDefault = arrayOf(
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(),
        )
        for (entry in available) {
            val view = layoutInflater.inflate(R.layout.item_setting, categoryList, false)
            view.backgroundTintList = ColorStateList(
                focusedSelectedDefault,
                intArrayOf(palette.text, Contrast.withAlpha(palette.text, SELECTED_ALPHA), Color.TRANSPARENT),
            )
            view.findViewById<TextView>(R.id.title).apply {
                setText(entry.title)
                setTextColor(ColorStateList(focusedSelectedDefault, intArrayOf(palette.onText, palette.text, palette.textMuted)))
            }
            view.findViewById<View>(R.id.value).visibility = View.GONE
            view.findViewById<View>(R.id.swatch).visibility = View.GONE
            view.isSelected = entry == category
            view.setOnFocusChangeListener { _, focused ->
                if (focused && category != entry) {
                    category = entry
                    categoryViews.forEach { (key, v) -> v.isSelected = key == entry }
                    render(focus = NO_FOCUS)
                }
            }
            view.setOnClickListener { render(focus = 0) }
            view.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                    focusableRows().firstOrNull()?.requestFocus()
                    true
                } else {
                    false
                }
            }
            categoryViews[entry] = view
            categoryList.addView(view)
        }
    }

    /** Shows the settings of the current category, and focuses the row at [focus] unless it is [NO_FOCUS]. */
    private fun render(focus: Int) {
        rows.removeAllViews()
        group = null
        val config = store.load()
        when (category) {
            Category.APPEARANCE -> appearance(config)
            Category.APPS -> apps(config)
            Category.REMOTE -> remote(config)
            Category.HOME_BUTTON -> homeButton()
            Category.SYSTEM -> system(config)
        }
        if (focus == NO_FOCUS) return
        val focusable = focusableRows()
        focusable.getOrNull(focus.coerceIn(0, (focusable.size - 1).coerceAtLeast(0)))?.requestFocus()
    }

    private fun appearance(config: LauncherConfig) {
        // A picture of the home screen that follows every change to theme, accent colour and tile corners.
        val density = resources.displayMetrics.density
        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setImageDrawable(ThemePreview(palette, config.accent, config.tileRadius))
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, PREVIEW_RADIUS_DP * density)
                }
            }
            clipToOutline = true
        }
        rows.addView(
            preview,
            LinearLayout.LayoutParams((PREVIEW_WIDTH_DP * density).toInt(), (PREVIEW_HEIGHT_DP * density).toInt()).apply {
                bottomMargin = (PREVIEW_GAP_DP * density).toInt()
            },
        )

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
        row(
            getString(R.string.settings_tile_shape),
            getString(R.string.tile_shape_value, tileShapeName(config.tileRadius), config.tileRadius),
            rerender = false,
        ) { anchor ->
            val index = focusableIndex(anchor)
            TileShapePicker(this, palette, config) { radius ->
                store.update { it.copy(tileRadius = radius) }
                if (!isFinishing && !isDestroyed) render(index)
            }.show(anchor)
        }
        val wallpaper = config.wallpaper
        val choices = backgroundChoices()
        val current = currentBackground(wallpaper, choices)
        val openBackgrounds = { anchor: View, focusDimming: Boolean ->
            val index = focusableIndex(anchor)
            BackgroundPicker(this, palette, config, choices, current, this::backgroundName) { next, dim ->
                store.update {
                    it.copy(
                        wallpaper = it.wallpaper.copy(
                            enabled = next != BACKGROUND_COLOR,
                            scene = if (next == BACKGROUND_IMAGE || next == BACKGROUND_COLOR) "" else next,
                            dim = dim,
                        ),
                    )
                }
                if (!isFinishing && !isDestroyed) render(index)
            }.show(anchor, focusDimming)
        }
        row(getString(R.string.settings_background), backgroundName(current), rerender = false) { anchor ->
            openBackgrounds(anchor, false)
        }
        if (current != BACKGROUND_COLOR) {
            row(getString(R.string.settings_wallpaper_dim), getString(R.string.percent, wallpaper.dim), rerender = false) { anchor ->
                openBackgrounds(anchor, true)
            }
        }
        row(getString(R.string.settings_columns), config.columns.toString()) {
            val next = if (config.columns >= MAX_COLUMNS_ON_TV) MIN_COLUMNS_ON_TV else config.columns + 1
            store.update { it.copy(columns = next) }
        }
    }

    private fun apps(config: LauncherConfig) {
        val catalog = AppCatalog(this)
        row(getString(R.string.settings_reset_order), null) {
            store.tileOrder = emptyList()
            Toast.makeText(this, R.string.settings_order_reset, Toast.LENGTH_SHORT).show()
        }
        for (pkg in config.hidden) {
            row(getString(R.string.settings_show_app, catalog.label(pkg)), getString(R.string.settings_hidden)) {
                store.update { it.copy(hidden = it.hidden - pkg) }
            }
        }
    }

    private fun remote(config: LauncherConfig) {
        val catalog = AppCatalog(this)
        if (config.shortcuts.isEmpty()) hint(getString(R.string.settings_shortcut_hint))
        for (key in LauncherConfig.SHORTCUT_KEYS.filter { it in config.shortcuts }) {
            val pkg = config.shortcuts.getValue(key)
            val title = getString(R.string.settings_shortcut, Shortcuts.label(this, key), catalog.label(pkg))
            row(title, getString(R.string.settings_remove)) {
                store.update { it.copy(shortcuts = it.shortcuts - key) }
            }
        }
    }

    private fun homeButton() {
        val on = Platform.redirectServiceEnabled(this)
        row(getString(R.string.settings_home_redirect), getString(if (on) R.string.on else R.string.off)) {
            AppLauncher.start(this, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun system(config: LauncherConfig) {
        val startup = config.startup
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_TV) || startup != StartupConfig()) {
            row(getString(R.string.settings_startup), startupName(startup, AppCatalog(this))) {
                val next = if (startup == StartupConfig()) StartupConfig(input = StartupConfig.LAST_INPUT) else StartupConfig()
                store.update { it.copy(startup = next) }
            }
        }
        row(getString(R.string.settings_tv_settings), null) {
            HomeRedirectService.passStockHomeOnce()
            AppLauncher.start(this, Intent(Settings.ACTION_SETTINGS))
        }
        row(getString(R.string.settings_about), getString(R.string.settings_version, versionName()), focusable = false) {}
    }

    /** A note in muted text, not a row: nothing to press. */
    private fun hint(text: String) {
        val density = resources.displayMetrics.density
        val padding = (HINT_PADDING_DP * density).toInt()
        val view = TextView(this).apply {
            this.text = text
            setTextColor(palette.textMuted)
            textSize = HINT_SP
            typeface = resources.getFont(R.font.geist)
            setPadding(padding, 0, padding, padding)
        }
        rows.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        group = null
    }

    private fun row(
        title: String,
        value: String?,
        swatch: Int? = null,
        focusable: Boolean = true,
        rerender: Boolean = true,
        action: (View) -> Unit,
    ) {
        val parent = group ?: card().also { group = it }
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

    /** Rows sit on a card: a glass card on Glass, the theme's surface colour otherwise. */
    private fun card(): LinearLayout {
        val density = resources.displayMetrics.density
        val radius = GROUP_RADIUS_DP * density
        val padding = (GROUP_PADDING_DP * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            val hairline = density.toInt().coerceAtLeast(1)
            background = if (palette.glass) {
                ThemeColors.edged(palette, radius, hairline)
            } else {
                ThemeColors.rounded(palette.surface, radius, palette.stroke, hairline)
            }
            rows.addView(this)
        }
    }

    private fun inContent(view: View?): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === rows) return true
            current = current.parent as? View
        }
        return false
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

    companion object {
        const val BACKGROUND_COLOR = "color"
        const val BACKGROUND_IMAGE = "image"
        private const val NO_FOCUS = -1
        private const val MIN_COLUMNS_ON_TV = 4
        private const val MAX_COLUMNS_ON_TV = 6
        private const val GROUP_RADIUS_DP = 18f
        private const val GROUP_PADDING_DP = 5f
        private const val PREVIEW_RADIUS_DP = 14f
        private const val PREVIEW_WIDTH_DP = 320f
        private const val PREVIEW_HEIGHT_DP = 180f
        private const val PREVIEW_GAP_DP = 24f
        private const val SELECTED_ALPHA = 0x1F
        private const val HINT_PADDING_DP = 20f
        private const val HINT_SP = 15f
    }
}
