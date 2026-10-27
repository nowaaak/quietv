package io.github.nowaaak.quietv.home

import android.view.KeyEvent
import io.github.nowaaak.quietv.config.LauncherConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsTest {

    @Test
    fun numberAndColourKeysHaveNames() {
        assertEquals("0", Shortcuts.keyName(KeyEvent.KEYCODE_0))
        assertEquals("9", Shortcuts.keyName(KeyEvent.KEYCODE_9))
        assertEquals("5", Shortcuts.keyName(KeyEvent.KEYCODE_NUMPAD_5))
        assertEquals("red", Shortcuts.keyName(KeyEvent.KEYCODE_PROG_RED))
        assertEquals("blue", Shortcuts.keyName(KeyEvent.KEYCODE_PROG_BLUE))
    }

    @Test
    fun otherKeysAreNotShortcuts() {
        assertNull(Shortcuts.keyName(KeyEvent.KEYCODE_DPAD_UP))
        assertNull(Shortcuts.keyName(KeyEvent.KEYCODE_BACK))
        assertNull(Shortcuts.keyName(KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun everyNameIsAValidConfigKey() {
        val codes = (KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) + listOf(
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_PROG_GREEN,
            KeyEvent.KEYCODE_PROG_YELLOW,
            KeyEvent.KEYCODE_PROG_BLUE,
        )
        codes.forEach { assertTrue(Shortcuts.keyName(it) in LauncherConfig.SHORTCUT_KEYS) }
    }
}
