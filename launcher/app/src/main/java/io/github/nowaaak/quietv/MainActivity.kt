package io.github.nowaaak.quietv

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.nowaaak.quietv.companion.CompanionReceiverService
import io.github.nowaaak.quietv.companion.CompanionSwitch
import io.github.nowaaak.quietv.config.CompanionMode
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.StartupConfig
import io.github.nowaaak.quietv.firetv.Platform
import io.github.nowaaak.quietv.home.AppCatalog
import io.github.nowaaak.quietv.home.AppLauncher
import io.github.nowaaak.quietv.home.Direction
import io.github.nowaaak.quietv.home.FocusRing
import io.github.nowaaak.quietv.home.GridNavigator
import io.github.nowaaak.quietv.home.SectionDecoration
import io.github.nowaaak.quietv.home.Shortcuts
import io.github.nowaaak.quietv.home.TileAdapter
import io.github.nowaaak.quietv.inputs.InputsMenu
import io.github.nowaaak.quietv.net.NetworkLabel
import io.github.nowaaak.quietv.search.SearchActivity
import io.github.nowaaak.quietv.settings.SettingsActivity
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.DropdownMenu
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.wallpaper.WallpaperStore
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : Activity(), TileAdapter.Listener {

    private lateinit var store: ConfigStore
    private lateinit var config: LauncherConfig
    private lateinit var palette: Palette
    private lateinit var catalog: AppCatalog
    private lateinit var navigator: GridNavigator
    private lateinit var adapter: TileAdapter
    private lateinit var grid: RecyclerView
    private lateinit var root: FrameLayout
    private lateinit var ring: FocusRing
    private lateinit var hero: View
    private lateinit var heroName: TextView
    private lateinit var heroEyebrow: TextView
    private lateinit var heroLine: View
    private lateinit var greeting: TextView
    private lateinit var wifiLabel: TextView
    private lateinit var wifiDot: View
    private lateinit var wallpaper: ImageView
    private lateinit var scrim: View
    private lateinit var springX: SpringAnimation
    private lateinit var springY: SpringAnimation

    private var columns = LauncherConfig.DEFAULT_COLUMNS
    private var density = 1f
    private var tileHeight = 0
    private var ringTargetY = 0f
    private var ringShown = false
    private var heroShown = false
    private var lastFocused = 0
    private var movingPos = -1
    private var tileMenu: DropdownMenu? = null
    private var afterLayout: (() -> Unit)? = null
    private var assigningPkg: String? = null
    private var tileRadius = 0f
    private var wallpaperVersion = NO_WALLPAPER
    private var wallpaperSamples = IntArray(0)

    private val handler = Handler(Looper.getMainLooper())
    private val minuteTick = object : Runnable {
        override fun run() {
            updateGreeting()
            updateNetwork()
            handler.postDelayed(this, MINUTE_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        store = ConfigStore(this)
        config = store.load()
        palette = Palette.of(config)
        ThemeColors.apply(this, palette)
        super.onCreate(savedInstanceState)
        Platform.syncHomeEntry(this)
        setContentView(R.layout.activity_main)

        density = resources.displayMetrics.density
        columns = config.columns
        tileRadius = config.tileRadius * density
        navigator = GridNavigator(columns)
        catalog = AppCatalog(this)

        root = findViewById(R.id.root)
        ring = findViewById(R.id.ring)
        hero = findViewById(R.id.hero)
        heroName = findViewById(R.id.heroName)
        heroEyebrow = findViewById(R.id.heroEyebrow)
        heroLine = findViewById(R.id.heroLine)
        greeting = findViewById(R.id.greeting)
        wifiLabel = findViewById(R.id.wifiLabel)
        wifiDot = findViewById(R.id.wifiDot)
        grid = findViewById(R.id.grid)
        wallpaper = findViewById(R.id.wallpaper)
        scrim = findViewById(R.id.scrim)

        val frame = grid.parent as View
        val column = frame.parent as View
        val content = resources.displayMetrics.widthPixels - column.paddingLeft - column.paddingRight
        val cell = content / columns.toFloat()
        val tileWidth = min(MAX_TILE_WIDTH_DP * density, cell - MIN_TILE_GAP_DP * density).roundToInt()
        tileHeight = (tileWidth * 9f / 16f).roundToInt()

        grid.layoutManager = object : GridLayoutManager(this, columns) {
            override fun onLayoutCompleted(state: RecyclerView.State) {
                super.onLayoutCompleted(state)
                val action = afterLayout ?: return
                afterLayout = null
                grid.post(action)
            }
        }
        grid.setHasFixedSize(true)
        grid.itemAnimator = null
        grid.addItemDecoration(SectionDecoration(this, columns, palette.textMuted))
        grid.layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(this, R.anim.tile_enter), 0.06f)
        adapter = TileAdapter(this, columns, tileWidth, tileHeight, palette, tileRadius)
        grid.adapter = adapter

        val bleed = if (columns < 2) 0 else ((cell - tileWidth) / 2 * columns / (columns - 1)).roundToInt()
        (frame.layoutParams as ViewGroup.MarginLayoutParams).apply {
            leftMargin = -max(0, bleed)
            rightMargin = -max(0, bleed)
            frame.layoutParams = this
        }

        grid.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val itemHeight = tileHeight +
                (TileAdapter.NAME_GAP_DP + TileAdapter.NAME_HEIGHT_DP + 2 * TileAdapter.VERTICAL_PADDING_DP) * density
            val dockRow = itemHeight + (SectionDecoration.LABEL_DP + SectionDecoration.DOCK_BOTTOM_DP) * density
            val want = max(0, (bottom - top - dockRow - DOCK_MARGIN_DP * density).toInt())
            if (grid.paddingTop != want) grid.setPadding(grid.paddingLeft, want, grid.paddingRight, grid.paddingBottom)
        }
        grid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!ringShown) return
                ringTargetY -= dy
                if (springY.isRunning) springY.animateToFinalPosition(ringTargetY) else ring.translationY = ringTargetY
            }
        })

        springX = SpringAnimation(ring, DynamicAnimation.TRANSLATION_X)
        springY = SpringAnimation(ring, DynamicAnimation.TRANSLATION_Y)
        listOf(springX, springY).forEach { it.spring = SpringForce().setStiffness(260f).setDampingRatio(0.58f) }

        ring.setPalette(palette)
        setUpTopBar()
        applyPalette()
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus?.id != R.id.card) hideRing()
            if (newFocus != null && newFocus.id != R.id.card) hideHero()
        }
    }

    private fun setUpTopBar() {
        val btnSearch = findViewById<View>(R.id.btnSearch)
        val btnWifi = findViewById<View>(R.id.btnWifi)
        val btnInputs = findViewById<ImageView>(R.id.btnInputs)
        val btnSettings = findViewById<View>(R.id.btnSettings)

        btnSearch.setOnClickListener { openSearch() }
        btnWifi.setOnClickListener { AppLauncher.start(this, Intent(Settings.ACTION_WIFI_SETTINGS)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        btnInputs.setOnClickListener {
            if (config.companion.mode == CompanionMode.SEND) {
                CompanionSwitch.send(config.companion) {
                    Toast.makeText(this, R.string.companion_failed, Toast.LENGTH_SHORT).show()
                }
            } else {
                InputsMenu(this, config.liveTvPackage, palette, config.accent, store).show(btnInputs, root)
            }
        }

        val edgeKeys = View.OnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> true
                KeyEvent.KEYCODE_DPAD_LEFT -> v.id == R.id.btnSearch
                KeyEvent.KEYCODE_DPAD_RIGHT -> v.id == R.id.btnSettings
                else -> false
            }
        }
        listOf(btnSearch, btnWifi, btnInputs, btnSettings).forEach { it.setOnKeyListener(edgeKeys) }
    }

    override fun onResume() {
        super.onResume()
        val fresh = store.load()
        if (fresh.theme != config.theme || fresh.colors != config.colors ||
            fresh.tileRadius != config.tileRadius || fresh.columns != config.columns
        ) {
            config = fresh
            recreate()
            return
        }
        config = fresh
        applyCompanionButton()
        applyAccent()
        applyWallpaper()
        CompanionReceiverService.sync(this, config)

        ringShown = false
        ring.alpha = 0f
        heroShown = false
        hero.alpha = 0f
        reload()
        grid.scheduleLayoutAnimation()
        updateGreeting()
        updateNetwork()
        handler.postDelayed(minuteTick, MINUTE_MS)
        runStartup()

        grid.post {
            val focused = currentFocus
            if (focused != null && grid.findContainingViewHolder(focused) != null) return@post
            focusTile(lastFocused)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(minuteTick)
        stopMoving()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) finish()
    }

    private fun reload() {
        adapter.submit(catalog.load(config, store.tileOrder))
    }

    private fun applyAccent() {
        val accent = ColorStateList.valueOf(config.accent)
        ring.setAccent(config.accent)
        wifiDot.backgroundTintList = accent
        heroLine.backgroundTintList = accent
        heroEyebrow.setTextColor(config.accent)
    }

    private fun applyPalette() {
        heroName.setTextColor(palette.text)
        findViewById<TextView>(R.id.clock).setTextColor(palette.text)
        findViewById<TextView>(R.id.okKey).apply {
            setTextColor(palette.text)
            background = ThemeColors.rounded(
                Color.TRANSPARENT,
                KEY_CAP_RADIUS_DP * density,
                palette.line,
                density.toInt().coerceAtLeast(1),
            )
        }
        findViewById<View>(R.id.hintDot).backgroundTintList = ColorStateList.valueOf(palette.line)
        listOf(R.id.greeting, R.id.openHint, R.id.holdHint, R.id.clockAmPm, R.id.date).forEach {
            findViewById<TextView>(it).setTextColor(palette.textMuted)
        }
        val content = ThemeColors.pillContent(palette)
        val pill = ThemeColors.pillBackground(palette)
        listOf(R.id.btnSearch, R.id.btnWifi, R.id.btnInputs, R.id.btnSettings).forEach {
            findViewById<View>(it).backgroundTintList = pill
        }
        listOf(R.id.btnSearch, R.id.btnInputs, R.id.btnSettings, R.id.wifiIcon).forEach {
            findViewById<ImageView>(it).imageTintList = content
        }
        wifiLabel.setTextColor(content)
    }

    private fun applyWallpaper() {
        val wallpapers = WallpaperStore(this)
        if (!config.wallpaper.enabled || !wallpapers.exists()) {
            wallpaperVersion = NO_WALLPAPER
            wallpaper.setImageDrawable(null)
            wallpaper.visibility = View.GONE
            scrim.visibility = View.GONE
            return
        }
        val version = wallpapers.version()
        if (version == wallpaperVersion) {
            applyScrim()
            return
        }
        wallpaperVersion = version
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        Thread {
            val bitmap = WallpaperStore.decode(wallpapers.file, width, height)
            val samples = bitmap?.let { WallpaperStore.samples(it) } ?: IntArray(0)
            runOnUiThread {
                if (isDestroyed || wallpaperVersion != version) return@runOnUiThread
                if (bitmap == null) {
                    wallpaper.visibility = View.GONE
                    scrim.visibility = View.GONE
                    return@runOnUiThread
                }
                wallpaperSamples = samples
                wallpaper.setImageBitmap(bitmap)
                wallpaper.visibility = View.VISIBLE
                applyScrim()
            }
        }.start()
    }

    private fun applyScrim() {
        val texts = intArrayOf(palette.text, palette.textMuted, palette.textDim)
        val amount = Contrast.scrimFor(wallpaperSamples, texts, palette.background, config.wallpaper.dim / 100f)
        scrim.setBackgroundColor(Contrast.withAlpha(palette.background, (amount * 255).roundToInt()))
        scrim.visibility = View.VISIBLE
    }

    private fun applyCompanionButton() {
        val button = findViewById<ImageView>(R.id.btnInputs)
        if (config.companion.mode == CompanionMode.SEND) {
            button.setImageResource(R.drawable.ic_livetv)
            button.contentDescription = getString(R.string.companion_switch)
        } else {
            button.setImageResource(R.drawable.ic_input)
            button.contentDescription = getString(R.string.inputs)
        }
    }

    override fun onTileClick(position: Int) {
        if (movingPos >= 0) return
        adapter.entryAt(position)?.let { AppLauncher.launch(this, it) }
    }

    override fun onTileFocused(position: Int, card: View) {
        lastFocused = position
        val r = Rect(0, 0, card.width, card.height)
        root.offsetDescendantRectToMyCoords(card, r)
        val pad = FocusRing.INSET_DP * density
        val w = card.width * TileAdapter.FOCUS_SCALE + 2 * pad
        val h = card.height * TileAdapter.FOCUS_SCALE + 2 * pad
        val x = r.exactCenterX() - w / 2f
        val y = r.exactCenterY() - h / 2f

        val lp = ring.layoutParams
        if (lp.width != w.toInt() || lp.height != h.toInt()) {
            lp.width = w.toInt()
            lp.height = h.toInt()
            ring.layoutParams = lp
            ring.setRadius(tileRadius * TileAdapter.FOCUS_SCALE)
        }
        ringTargetY = y
        if (!ringShown) {
            springX.cancel()
            springY.cancel()
            ring.translationX = x
            ring.translationY = y
            ring.animate().alpha(1f).setDuration(180).start()
            ringShown = true
        } else {
            springX.animateToFinalPosition(x)
            springY.animateToFinalPosition(y)
        }

        val item = card.parent as View
        if (navigator.isDock(position)) {
            val decoratedTop = item.top - (6 * density).toInt()
            val dy = decoratedTop - grid.paddingTop
            if (abs(dy) > 1) grid.smoothScrollBy(0, dy)
            showHero(position, (r.top - dy).toFloat())
        } else {
            hideHero()
            val want = Rect(0, -(pad.toInt()) * 2, item.width, item.height + pad.toInt())
            grid.requestChildRectangleOnScreen(item, want, false)
        }
    }

    override fun onTileKey(position: Int, card: View, event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val select = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
        if (movingPos < 0) {
            if (event.action != KeyEvent.ACTION_DOWN) return false
            if (select && event.repeatCount > 0) {
                card.cancelLongPress()
                card.isPressed = false
                onTileMenu(position, card)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                onTileMenu(position, card)
                return true
            }
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
                KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
                else -> return false
            }
            val target = navigator.vertical(position, adapter.itemCount, direction) ?: return false
            if (target != position) focusTile(target)
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (select && event.repeatCount > 0) return true
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
            KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> {
                stopMoving()
                return true
            }
            else -> return true
        }
        val target = navigator.moveTarget(position, adapter.itemCount, direction) ?: return true
        afterLayout = {
            val moved = grid.findViewHolderForAdapterPosition(target)?.itemView?.findViewById<View>(R.id.card)
            if (moved == null) {
                focusTile(target)
            } else {
                moved.requestFocus()
                onTileFocused(target, moved)
            }
        }
        adapter.move(position, target)
        movingPos = target
        return true
    }

    override fun onTileMenu(position: Int, card: View) {
        if (movingPos >= 0 || tileMenu?.isShowing == true) return
        val entry = adapter.entryAt(position) ?: return
        val menu = DropdownMenu(this, root, palette, config.accent)
        menu.addRow(R.drawable.ic_move, getString(R.string.tile_move)) {
            movingPos = position
            ring.setMoving(true)
            card.requestFocus()
        }
        if (!entry.isStockHome) {
            menu.addRow(R.drawable.ic_hide, getString(R.string.tile_hide)) {
                store.update { it.copy(hidden = (it.hidden + entry.pkg).distinct()) }
                config = store.load()
                reload()
                grid.post { focusTile(position) }
                Toast.makeText(this, getString(R.string.tile_hidden, entry.label), Toast.LENGTH_SHORT).show()
            }
            menu.addRow(R.drawable.ic_keypad, getString(R.string.tile_shortcut)) {
                assigningPkg = entry.pkg
                Toast.makeText(this, getString(R.string.shortcut_prompt, entry.label), Toast.LENGTH_LONG).show()
            }
            menu.addRow(R.drawable.ic_info, getString(R.string.tile_info)) { AppLauncher.openDetails(this, entry.pkg) }
        }
        menu.onDismiss = { tileMenu = null }
        tileMenu = menu
        menu.show(card, Gravity.START, TILE_MENU_GAP_DP)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return super.dispatchKeyEvent(event)
        val key = Shortcuts.keyName(event.keyCode)
        val pending = assigningPkg
        if (pending != null) {
            assigningPkg = null
            if (key == null) {
                Toast.makeText(this, R.string.shortcut_cancelled, Toast.LENGTH_SHORT).show()
            } else {
                store.update { it.copy(shortcuts = it.shortcuts + (key to pending)) }
                config = store.load()
                val text = getString(R.string.shortcut_set, Shortcuts.label(this, key), catalog.label(pending))
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_SEARCH) {
            openSearch()
            return true
        }
        val pkg = key?.let { config.shortcuts[it] }
        if (pkg != null && movingPos < 0 && tileMenu?.isShowing != true) {
            openPackage(pkg)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openSearch() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private fun openPackage(pkg: String) {
        val entry = adapter.entries.firstOrNull { it.pkg == pkg }
        if (entry != null) {
            AppLauncher.launch(this, entry)
            return
        }
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.cant_open, catalog.label(pkg)), Toast.LENGTH_SHORT).show()
        } else {
            AppLauncher.start(this, intent)
        }
    }

    private fun runStartup() {
        val boot = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        if (boot < 0 || boot == store.startupBoot) return
        store.startupBoot = boot
        if (SystemClock.elapsedRealtime() > STARTUP_WINDOW_MS) return
        val startup = config.startup
        when {
            startup.app.isNotEmpty() -> openPackage(startup.app)
            startup.input.isNotEmpty() -> {
                val id = if (startup.input == StartupConfig.LAST_INPUT) store.lastInput else startup.input
                if (id.isNotEmpty()) AppLauncher.start(this, InputsMenu.inputIntent(id))
            }
        }
    }

    private fun stopMoving() {
        if (movingPos < 0) return
        movingPos = -1
        ring.setMoving(false)
        store.tileOrder = adapter.entries.map { it.pkg }
    }

    private fun focusTile(position: Int) {
        if (adapter.itemCount == 0) return
        val pos = position.coerceIn(0, adapter.itemCount - 1)
        val holder = grid.findViewHolderForAdapterPosition(pos)
        if (holder != null) {
            holder.itemView.findViewById<View>(R.id.card).requestFocus()
            return
        }
        grid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                rv.removeOnScrollListener(this)
                val target = rv.findViewHolderForAdapterPosition(pos)?.itemView?.findViewById<View>(R.id.card)
                    ?: rv.getChildAt(0)
                target?.requestFocus()
            }
        })
        grid.smoothScrollToPosition(pos)
    }

    private fun showHero(position: Int, dockTopInRoot: Float) {
        val label = adapter.entryAt(position)?.label.orEmpty()
        val y = dockTopInRoot - (8 + SectionDecoration.LABEL_DP + 18) * density - hero.height
        when {
            !heroShown -> {
                heroName.text = label
                hero.y = y
                heroName.translationX = 0f
                heroName.alpha = 1f
                hero.animate().alpha(1f).setDuration(260).start()
                heroShown = true
            }
            label != heroName.text.toString() -> {
                heroName.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction {
                    heroName.text = label
                    hero.y = y
                    heroName.translationX = 12 * density
                    heroName.animate().alpha(1f).translationX(0f).setDuration(200).start()
                }.start()
            }
            else -> hero.y = y
        }
    }

    private fun hideHero() {
        if (!heroShown) return
        heroShown = false
        hero.animate().alpha(0f).setDuration(160).start()
    }

    private fun hideRing() {
        if (!ringShown) return
        ringShown = false
        ring.animate().alpha(0f).setDuration(160).start()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greeting.setText(
            when {
                hour < 5 -> R.string.good_night
                hour < 12 -> R.string.good_morning
                hour < 17 -> R.string.good_afternoon
                hour < 22 -> R.string.good_evening
                else -> R.string.good_night
            },
        )
    }

    private fun updateNetwork() {
        val status = NetworkLabel.read(this)
        wifiLabel.text = status.label
        wifiDot.visibility = if (status.online) View.VISIBLE else View.GONE
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val NO_WALLPAPER = -1L
        const val STARTUP_WINDOW_MS = 5 * 60_000L
        const val KEY_CAP_RADIUS_DP = 5f
        const val MAX_TILE_WIDTH_DP = 148f
        const val MIN_TILE_GAP_DP = 20f
        const val DOCK_MARGIN_DP = 64f
        const val TILE_MENU_GAP_DP = 18f
    }
}
