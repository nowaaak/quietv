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
) {
    GRAPHITE(
        "graphite", false,
        0xFF0E1014.toInt(), 0xFF171A20.toInt(), 0xFFF3F4F6.toInt(),
        0xFF9AA1AD.toInt(), 0xFF8B93A0.toInt(), 0xFF3A404B.toInt(),
    ),
    PAPER(
        "paper", true,
        0xFFF4F4F1.toInt(), 0xFFFFFFFF.toInt(), 0xFF15171C.toInt(),
        0xFF5F6673.toInt(), 0xFF676E7B.toInt(), 0xFFD4D7DD.toInt(),
    ),
    MIDNIGHT(
        "midnight", false,
        0xFF000000.toInt(), 0xFF121316.toInt(), 0xFFF5F5F5.toInt(),
        0xFFA1A1A8.toInt(), 0xFF8F8F96.toInt(), 0xFF2E2F33.toInt(),
    ),
    SLATE(
        "slate", false,
        0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFFF1F5F9.toInt(),
        0xFFA3B1C6.toInt(), 0xFF94A3B8.toInt(), 0xFF334155.toInt(),
    ),
    FOREST(
        "forest", false,
        0xFF0B1510.toInt(), 0xFF15231C.toInt(), 0xFFEBF3EE.toInt(),
        0xFF9DB2A5.toInt(), 0xFF8CA295.toInt(), 0xFF2B3D33.toInt(),
    ),
    SAND(
        "sand", true,
        0xFFF2ECE2.toInt(), 0xFFFFFCF5.toInt(), 0xFF2A231B.toInt(),
        0xFF685C4E.toInt(), 0xFF6F6355.toInt(), 0xFFDAD0C0.toInt(),
    ),
    ;

    companion object {
        private val LEGACY = mapOf("dark" to GRAPHITE, "light" to PAPER)

        fun fromKey(key: String): ThemePreset? = entries.firstOrNull { it.key == key } ?: LEGACY[key]
    }
}
