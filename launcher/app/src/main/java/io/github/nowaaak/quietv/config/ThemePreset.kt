package io.github.nowaaak.quietv.config

enum class ThemePreset(
    val key: String,
    val light: Boolean,
    val background: Int,
    val surface: Int,
    val text: Int,
    val textMuted: Int,
    val textDim: Int,
    val line: Int,
    val glass: Boolean = false,
    val backgroundEnd: Int = background,
    val glow: Int = 0,
) {
    GRAPHITE(
        "graphite", false,
        0xFF0E1014.toInt(), 0xFF171A20.toInt(), 0xFFF3F4F6.toInt(),
        0xFF9AA1AD.toInt(), 0xFF8B93A0.toInt(), 0xFF3A404B.toInt(),
    ),
    SAND(
        "sand", true,
        0xFFF2ECE2.toInt(), 0xFFFFFCF5.toInt(), 0xFF2A231B.toInt(),
        0xFF685C4E.toInt(), 0xFF6F6355.toInt(), 0xFFDAD0C0.toInt(),
    ),
    GLASS(
        "glass", false,
        0xFF0B0B0D.toInt(), 0xFF1C1D21.toInt(), 0xFFF5F5F7.toInt(),
        0xFFA1A1AA.toInt(), 0xFF8E8E96.toInt(), 0xFF2E2F35.toInt(),
        glass = true,
        backgroundEnd = 0xFF18191C.toInt(),
        glow = 0x14FFFFFF,
    ),
    ;

    companion object {
        // Names of themes that were removed or renamed, so older configs keep working.
        private val LEGACY = mapOf(
            "dark" to GRAPHITE, "light" to SAND, "paper" to SAND,
            "midnight" to GRAPHITE, "slate" to GRAPHITE, "forest" to GRAPHITE,
        )

        fun fromKey(key: String): ThemePreset? = entries.firstOrNull { it.key == key } ?: LEGACY[key]
    }
}
