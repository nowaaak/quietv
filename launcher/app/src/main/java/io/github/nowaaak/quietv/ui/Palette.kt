package io.github.nowaaak.quietv.ui

import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.ThemePreset

data class Palette(
    val light: Boolean,
    val background: Int,
    val surface: Int,
    val text: Int,
    val textMuted: Int,
    val textDim: Int,
    val line: Int,
) {
    val stroke: Int get() = Contrast.withAlpha(text, if (light) 0x1A else 0x14)
    val onText: Int get() = background
    val onTextDim: Int get() = Contrast.withAlpha(background, 0xB3)

    companion object {
        fun of(config: LauncherConfig): Palette = of(config.theme, config.colors)

        fun of(theme: ThemePreset, colors: Map<String, Int>): Palette {
            val background = colors[LauncherConfig.COLOR_BACKGROUND] ?: theme.background
            val muted = colors[LauncherConfig.COLOR_TEXT_MUTED]
            return Palette(
                light = if (LauncherConfig.COLOR_BACKGROUND in colors) {
                    Contrast.luminance(background) > LIGHT_THRESHOLD
                } else {
                    theme.light
                },
                background = background,
                surface = colors[LauncherConfig.COLOR_SURFACE] ?: theme.surface,
                text = colors[LauncherConfig.COLOR_TEXT] ?: theme.text,
                textMuted = muted ?: theme.textMuted,
                textDim = muted ?: theme.textDim,
                line = colors[LauncherConfig.COLOR_LINE] ?: theme.line,
            )
        }

        private const val LIGHT_THRESHOLD = 0.4
    }
}
