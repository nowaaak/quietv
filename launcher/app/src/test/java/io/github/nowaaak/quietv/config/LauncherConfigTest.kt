package io.github.nowaaak.quietv.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherConfigTest {

    @Test
    fun documentedExamplesAreValid() {
        val examples = File("../../docs/examples").listFiles { file -> file.extension == "json" }.orEmpty()
        assertTrue("no examples found", examples.isNotEmpty())
        examples.forEach { LauncherConfig.parse(it.readText()) }
    }

    @Test
    fun emptyObjectGivesDefaults() {
        assertEquals(LauncherConfig(), LauncherConfig.parse("{}"))
    }

    @Test
    fun roundTripKeepsEveryField() {
        val config = LauncherConfig(
            favorites = listOf("com.netflix.ninja", "com.google.android.youtube.tv"),
            hidden = listOf("com.mediatek.wwtv.tvcenter"),
            extraApps = listOf("com.amazon.cloud9"),
            columns = 4,
            accent = 0xFF93C5FD.toInt(),
            theme = ThemePreset.SAND,
            colors = mapOf("background" to 0xFF101820.toInt(), "textMuted" to 0xFFA0A8B0.toInt()),
            tileRadius = 6,
            wallpaper = WallpaperConfig(enabled = true, dim = 45),
            liveTvPackage = "com.mediatek.wwtv.tvcenter",
            fireTv = FireTvConfig(redirectHome = false, stockHomePackage = "com.amazon.tv.launcher", showStockHomeTile = true),
            companion = CompanionConfig(CompanionMode.SEND, "192.168.1.20", 47202, "abcdefghijklmnop"),
        )
        assertEquals(config, LauncherConfig.parse(config.toJson()))
    }

    @Test
    fun duplicatePackagesAreCollapsed() {
        val config = LauncherConfig.parse("""{"favorites": ["com.a.b", "com.a.b", "com.c.d"]}""")
        assertEquals(listOf("com.a.b", "com.c.d"), config.favorites)
    }

    @Test
    fun invalidValuesAreAllReported() {
        val error = assertThrows(InvalidConfigException::class.java) {
            LauncherConfig.parse(
                """{"columns": 12, "accent": "green", "theme": "blue", "dock": [], "favorites": ["x; reboot"]}""",
            )
        }
        val message = error.message.orEmpty()
        listOf("columns", "accent", "theme", "unknown key 'dock'", "favorites").forEach {
            assertTrue("missing '$it' in: $message", message.contains(it))
        }
    }

    @Test
    fun legacyThemeNamesStillWork() {
        assertEquals(ThemePreset.GRAPHITE, LauncherConfig.parse("""{"theme": "dark"}""").theme)
        assertEquals(ThemePreset.PAPER, LauncherConfig.parse("""{"theme": "light"}""").theme)
        assertEquals(ThemePreset.MIDNIGHT, LauncherConfig.parse("""{"theme": "midnight"}""").theme)
    }

    @Test
    fun appearanceValuesAreChecked() {
        val error = assertThrows(InvalidConfigException::class.java) {
            LauncherConfig.parse(
                """{"colors": {"text": "white", "glow": "#FFFFFF"}, "tileRadius": 40,
                   "wallpaper": {"enabled": "yes", "dim": 95, "blur": 3}}""",
            )
        }
        val message = error.message.orEmpty()
        listOf("colors.text", "unknown key 'colors.glow'", "tileRadius", "wallpaper.enabled", "wallpaper.dim",
            "unknown key 'wallpaper.blur'").forEach {
            assertTrue("missing '$it' in: $message", message.contains(it))
        }
    }

    @Test
    fun companionNeedsAToken() {
        assertThrows(InvalidConfigException::class.java) {
            LauncherConfig.parse("""{"companion": {"mode": "receive", "token": "short"}}""")
        }
        assertThrows(InvalidConfigException::class.java) {
            LauncherConfig.parse("""{"companion": {"mode": "send", "token": "abcdefghijklmnop"}}""")
        }
        val ok = LauncherConfig.parse("""{"companion": {"mode": "receive", "token": "abcdefghijklmnop"}}""")
        assertEquals(CompanionMode.RECEIVE, ok.companion.mode)
    }

    @Test
    fun notJsonIsRejected() {
        assertThrows(InvalidConfigException::class.java) { LauncherConfig.parse("columns=5") }
    }

    @Test
    fun colorsParseAndFormat() {
        assertEquals(0xFF6EE7B7.toInt(), LauncherConfig.parseColor("#6ee7b7"))
        assertEquals("#6EE7B7", LauncherConfig.formatColor(LauncherConfig.DEFAULT_ACCENT))
        assertNull(LauncherConfig.parseColor("#6EE7B"))
    }

    @Test
    fun packageNames() {
        assertTrue(LauncherConfig.isPackageName("com.netflix.ninja"))
        assertFalse(LauncherConfig.isPackageName("netflix"))
        assertFalse(LauncherConfig.isPackageName("com.a;rm -rf"))
    }
}
