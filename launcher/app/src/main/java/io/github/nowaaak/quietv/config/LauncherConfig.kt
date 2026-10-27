package io.github.nowaaak.quietv.config

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class InvalidConfigException(message: String) : Exception(message)

enum class ThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
}

enum class CompanionMode(val key: String) {
    OFF("off"),
    SEND("send"),
    RECEIVE("receive"),
}

data class FireTvConfig(
    val redirectHome: Boolean = true,
    val stockHomePackage: String = "",
    val stockHomeClassHint: String = DEFAULT_CLASS_HINT,
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

data class LauncherConfig(
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val extraApps: List<String> = emptyList(),
    val columns: Int = DEFAULT_COLUMNS,
    val accent: Int = DEFAULT_ACCENT,
    val theme: ThemeMode = ThemeMode.DARK,
    val liveTvPackage: String = "",
    val fireTv: FireTvConfig = FireTvConfig(),
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
        .put("liveTvPackage", liveTvPackage)
        .put(
            "fireTv",
            JSONObject()
                .put("redirectHome", fireTv.redirectHome)
                .put("stockHomePackage", fireTv.stockHomePackage)
                .put("stockHomeClassHint", fireTv.stockHomeClassHint)
                .put("showStockHomeTile", fireTv.showStockHomeTile),
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

        private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        private val COLOR = Regex("^#[0-9A-Fa-f]{6}$")
        private val HOST = Regex("^[A-Za-z0-9.:-]+$")
        private val TOKEN = Regex("^[A-Za-z0-9_-]{${CompanionConfig.MIN_TOKEN_LENGTH},}$")
        private val TOP_KEYS = setOf(
            "version", "favorites", "hidden", "extraApps", "columns", "accent", "theme", "liveTvPackage",
            "fireTv", "companion",
        )
        private val FIRE_TV_KEYS = setOf("redirectHome", "stockHomePackage", "stockHomeClassHint", "showStockHomeTile")
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
            val theme = ThemeMode.entries.firstOrNull { it.key == root.optString("theme", ThemeMode.DARK.key) }
            if (theme == null) problems += "theme must be 'dark' or 'light'"
            val liveTv = optionalPackage(root, "liveTvPackage", "liveTvPackage", problems)

            val fireJson = objectOrEmpty(root, "fireTv", problems)
            unknownKeys(fireJson, FIRE_TV_KEYS, "fireTv.", problems)
            val redirect = fireJson.opt("redirectHome") ?: true
            if (redirect !is Boolean) problems += "fireTv.redirectHome must be true or false"
            val stockTile = fireJson.opt("showStockHomeTile") ?: false
            if (stockTile !is Boolean) problems += "fireTv.showStockHomeTile must be true or false"
            val fireTv = FireTvConfig(
                redirectHome = redirect as? Boolean ?: true,
                stockHomePackage = optionalPackage(fireJson, "stockHomePackage", "fireTv.stockHomePackage", problems),
                stockHomeClassHint = fireJson.optString("stockHomeClassHint", FireTvConfig.DEFAULT_CLASS_HINT)
                    .ifBlank { FireTvConfig.DEFAULT_CLASS_HINT },
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
                liveTvPackage = liveTv,
                fireTv = fireTv,
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
