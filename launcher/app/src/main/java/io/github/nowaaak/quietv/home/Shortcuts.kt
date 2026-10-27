package io.github.nowaaak.quietv.home

import android.content.Context
import android.view.KeyEvent
import io.github.nowaaak.quietv.R

object Shortcuts {

    private val COLOURS = mapOf(
        KeyEvent.KEYCODE_PROG_RED to "red",
        KeyEvent.KEYCODE_PROG_GREEN to "green",
        KeyEvent.KEYCODE_PROG_YELLOW to "yellow",
        KeyEvent.KEYCODE_PROG_BLUE to "blue",
    )

    fun keyName(keyCode: Int): String? = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> (keyCode - KeyEvent.KEYCODE_0).toString()
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> (keyCode - KeyEvent.KEYCODE_NUMPAD_0).toString()
        else -> COLOURS[keyCode]
    }

    fun label(context: Context, key: String): String = when (key) {
        "red" -> context.getString(R.string.key_red)
        "green" -> context.getString(R.string.key_green)
        "yellow" -> context.getString(R.string.key_yellow)
        "blue" -> context.getString(R.string.key_blue)
        else -> context.getString(R.string.key_number, key)
    }
}
