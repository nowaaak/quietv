package io.github.nowaaak.quietv.config

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class InvalidConfigException(message: String) : Exception(message)

enum class CompanionMode(val key: String) {
    OFF("off"),
    SEND("send"),
    RECEIVE("receive"),
}

data class HomeRedirectConfig(
    val enabled: Boolean = true,
    val stockHomePackage: String = "",
    val classHint: String = DEFAULT_CLASS_HINT,
    val showStockHomeTile: Boolean = false,
) {
    companion object {
        const val DEFAULT_CLASS_HINT = "HomeActivity"
    }
}

data class CompanionConfig(
    val mode: CompanionMode = CompanionMode.OFF,
    val host: String = "",
    val port: Int = DEFAULT_PORT,
    val token: String = "",
) {
    companion object {
        const val DEFAULT_PORT = 47201
        const val MIN_TOKEN_LENGTH = 16
    }
}

data class WallpaperConfig(
    val enabled: Boolean = false,
    val dim: Int = DEFAULT_DIM,
) {
    companion object {
        const val DEFAULT_DIM = 30
        const val MAX_DIM = 80
    }
}

data class LauncherConfig(
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val extraApps: List<String> = emptyList(),
    val columns: Int = DEFAULT_COLUMNS,
    val accent: Int = DEFAULT_ACCENT,
    val theme: ThemePreset = ThemePreset.GRAPHITE,
    val colors: Map<String, Int> = emptyMap(),
    val tileRadius: Int = DEFAULT_TILE_RADIUS,
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val liveTvPackage: String = "",
    val homeRedirect: HomeRedirectConfig = HomeRedirectConfig(),
    val companion: CompanionConfig = CompanionConfig(),
) {

    fun toJson(): String = JSONObject()
        .put("version", VERSION)
        .put("favorites", JSONArray(favorites))
        .put("hidden", JSONArray(hidden))
        .put("extraApps", JSONArray(extraApps))
        .put("columns", columns)
        .put("accent", formatColor(accent))
        .put("theme", theme.key)
        .put("colors", JSONObject().also { json -> colors.toSortedMap().forEach { (k, v) -> json.put(k, formatColor(v)) } })
        .put("tileRadius", tileRadius)
        .put("wallpaper", JSONObject().put("enabled", wallpaper.enabled).put("dim", wallpaper.dim))
        .put("liveTvPackage", liveTvPackage)
        .put(
            "homeRedirect",
            JSONObject()
                .put("enabled", homeRedirect.enabled)
                .put("stockHomePackage", homeRedirect.stockHomePackage)
                .put("classHint", homeRedirect.classHint)
                .put("showStockHomeTile", homeRedirect.showStockHomeTile),
        )
        .put(
            "companion",
            JSONObject()
                .put("mode", companion.mode.key)
                .put("host", companion.host)
                .put("port", companion.port)
                .put("token", companion.token),
        )
        .toString()

    companion object {
        const val VERSION = 1
        const val DEFAULT_COLUMNS = 5
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 8
        const val DEFAULT_ACCENT = 0xFF6EE7B7.toInt()
        const val DEFAULT_TILE_RADIUS = 14
        const val MAX_TILE_RADIUS = 24
        const val COLOR_BACKGROUND = "background"
        const val COLOR_SURFACE = "surface"
        const val COLOR_TEXT = "text"
        const val COLOR_TEXT_MUTED = "textMuted"
        const val COLOR_LINE = "line"
        val COLOR_KEYS = setOf(COLOR_BACKGROUND, COLOR_SURFACE, COLOR_TEXT, COLOR_TEXT_MUTED, COLOR_LINE)

        private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        private val COLOR = Regex("^#[0-9A-Fa-f]{6}$")
        private val HOST = Regex("^[A-Za-z0-9.:-]+$")
        private val TOKEN = Regex("^[A-Za-z0-9_-]{${CompanionConfig.MIN_TOKEN_LENGTH},}$")
        private val TOP_KEYS = setOf(
            "version", "favorites", "hidden", "extraApps", "columns", "accent", "theme", "colors", "tileRadius",
            "wallpaper", "liveTvPackage", "homeRedirect", "fireTv", "companion",
        )
        private val WALLPAPER_KEYS = setOf("enabled", "dim")
        private val REDIRECT_KEYS = setOf("enabled", "stockHomePackage", "classHint", "showStockHomeTile")
        private val LEGACY_REDIRECT_KEYS = mapOf(
            "redirectHome" to "enabled",
            "stockHomePackage" to "stockHomePackage",
            "stockHomeClassHint" to "classHint",
            "showStockHomeTile" to "showStockHomeTile",
        )
        private val COMPANION_KEYS = setOf("mode", "host", "port", "token")

        fun isPackageName(value: String): Boolean = PACKAGE.matches(value)

        fun formatColor(color: Int): String = String.format("#%06X", color and 0xFFFFFF)

        fun parseColor(value: String): Int? =
            if (COLOR.matches(value)) (0xFF000000L or value.substring(1).toLong(16)).toInt() else null

        fun parse(json: String): LauncherConfig {
            val root = try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw InvalidConfigException("not a JSON object: ${e.message}")
            }
            val problems = mutableListOf<String>()
            unknownKeys(root, TOP_KEYS, "", problems)
            if (root.optInt("version", VERSION) != VERSION) problems += "version must be $VERSION"

            val columns = root.opt("columns") ?: DEFAULT_COLUMNS
            if (columns !is Int || columns !in MIN_COLUMNS..MAX_COLUMNS) {
                problems += "columns must be a whole number from $MIN_COLUMNS to $MAX_COLUMNS"
            }
            val accent = parseColor(root.optString("accent", formatColor(DEFAULT_ACCENT)))
            if (accent == null) problems += "accent must look like #RRGGBB"
            val theme = ThemePreset.fromKey(root.optString("theme", ThemePreset.GRAPHITE.key))
            if (theme == null) problems += "theme must be one of ${ThemePreset.entries.joinToString { it.key }}"
            val colors = colors(root, problems)
            val tileRadius = root.opt("tileRadius") ?: DEFAULT_TILE_RADIUS
            if (tileRadius !is Int || tileRadius !in 0..MAX_TILE_RADIUS) {
                problems += "tileRadius must be a whole number from 0 to $MAX_TILE_RADIUS"
            }
            val wallpaperJson = objectOrEmpty(root, "wallpaper", problems)
            unknownKeys(wallpaperJson, WALLPAPER_KEYS, "wallpaper.", problems)
            val wallpaperOn = wallpaperJson.opt("enabled") ?: false
            if (wallpaperOn !is Boolean) problems += "wallpaper.enabled must be true or false"
            val dim = wallpaperJson.opt("dim") ?: WallpaperConfig.DEFAULT_DIM
            if (dim !is Int || dim !in 0..WallpaperConfig.MAX_DIM) {
                problems += "wallpaper.dim must be a whole number from 0 to ${WallpaperConfig.MAX_DIM}"
            }
            val liveTv = optionalPackage(root, "liveTvPackage", "liveTvPackage", problems)

            val redirectJson = homeRedirectJson(root, problems)
            unknownKeys(redirectJson, REDIRECT_KEYS, "homeRedirect.", problems)
            val redirect = redirectJson.opt("enabled") ?: true
            if (redirect !is Boolean) problems += "homeRedirect.enabled must be true or false"
            val stockTile = redirectJson.opt("showStockHomeTile") ?: false
            if (stockTile !is Boolean) problems += "homeRedirect.showStockHomeTile must be true or false"
            val homeRedirect = HomeRedirectConfig(
                enabled = redirect as? Boolean ?: true,
                stockHomePackage = optionalPackage(
                    redirectJson,
                    "stockHomePackage",
                    "homeRedirect.stockHomePackage",
                    problems,
                ),
                classHint = redirectJson.optString("classHint", HomeRedirectConfig.DEFAULT_CLASS_HINT)
                    .ifBlank { HomeRedirectConfig.DEFAULT_CLASS_HINT },
                showStockHomeTile = stockTile as? Boolean ?: false,
            )

            val companionJson = objectOrEmpty(root, "companion", problems)
            unknownKeys(companionJson, COMPANION_KEYS, "companion.", problems)
            val mode = CompanionMode.entries.firstOrNull { it.key == companionJson.optString("mode", "off") }
            if (mode == null) problems += "companion.mode must be 'off', 'send' or 'receive'"
            val port = companionJson.opt("port") ?: CompanionConfig.DEFAULT_PORT
            if (port !is Int || port !in 1024..65535) problems += "companion.port must be from 1024 to 65535"
            val token = companionJson.optString("token", "")
            val host = companionJson.optString("host", "")
            if (mode != null && mode != CompanionMode.OFF && !TOKEN.matches(token)) {
                problems += "companion.token needs at least ${CompanionConfig.MIN_TOKEN_LENGTH} letters, digits, - or _"
            }
            if (mode == CompanionMode.SEND && !HOST.matches(host)) {
                problems += "companion.host is required when companion.mode is 'send'"
            }

            val favorites = packages(root, "favorites", problems)
            val hidden = packages(root, "hidden", problems)
            val extraApps = packages(root, "extraApps", problems)
            if (problems.isNotEmpty()) throw InvalidConfigException(problems.joinToString("; "))

            return LauncherConfig(
                favorites = favorites,
                hidden = hidden,
                extraApps = extraApps,
                columns = columns as Int,
                accent = accent!!,
                theme = theme!!,
                colors = colors,
                tileRadius = tileRadius as Int,
                wallpaper = WallpaperConfig(wallpaperOn as Boolean, dim as Int),
                liveTvPackage = liveTv,
                homeRedirect = homeRedirect,
                companion = CompanionConfig(mode!!, host, port as Int, token),
            )
        }

        private fun unknownKeys(obj: JSONObject, allowed: Set<String>, prefix: String, problems: MutableList<String>) {
            obj.keys().asSequence().filter { it !in allowed }.sorted().forEach { problems += "unknown key '$prefix$it'" }
        }

        private fun objectOrEmpty(root: JSONObject, key: String, problems: MutableList<String>): JSONObject {
            val value = root.opt(key) ?: return JSONObject()
            if (value is JSONObject) return value
            problems += "$key must be an object"
            return JSONObject()
        }

        private fun optionalPackage(obj: JSONObject, key: String, name: String, problems: MutableList<String>): String {
            val value = obj.optString(key, "")
            if (value.isNotEmpty() && !isPackageName(value)) problems += "$name must be a package name or empty"
            return value
        }

        private fun homeRedirectJson(root: JSONObject, problems: MutableList<String>): JSONObject {
            if (!root.has("fireTv")) return objectOrEmpty(root, "homeRedirect", problems)
            if (root.has("homeRedirect")) {
                problems += "use homeRedirect, not the old fireTv key"
                return JSONObject()
            }
            val legacy = objectOrEmpty(root, "fireTv", problems)
            val result = JSONObject()
            for (key in legacy.keys()) {
                val name = LEGACY_REDIRECT_KEYS[key]
                if (name == null) problems += "unknown key 'fireTv.$key'" else result.put(name, legacy.get(key))
            }
            return result
        }

        private fun colors(root: JSONObject, problems: MutableList<String>): Map<String, Int> {
            val json = objectOrEmpty(root, "colors", problems)
            unknownKeys(json, COLOR_KEYS, "colors.", problems)
            val result = mutableMapOf<String, Int>()
            for (key in json.keys().asSequence().filter { it in COLOR_KEYS }) {
                val color = (json.opt(key) as? String)?.let { parseColor(it) }
                if (color == null) problems += "colors.$key must look like #RRGGBB" else result[key] = color
            }
            return result
        }

        private fun packages(root: JSONObject, key: String, problems: MutableList<String>): List<String> {
            val value = root.opt(key) ?: return emptyList()
            val array = value as? JSONArray
            val names = array?.let { a -> (0 until a.length()).map { a.opt(it) } }
            if (names == null || names.any { it !is String || !isPackageName(it) }) {
                problems += "$key must be a list of package names"
                return emptyList()
            }
            return names.map { it as String }.distinct()
        }
    }
}
