package io.github.nowaaak.quietv.firetv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantGraceTest {

    @Test
    fun assistantWindowPausesTheRedirect() {
        val grace = AssistantGrace(graceMs = 8000)
        assertFalse(grace.active(1000))
        grace.windowShown("com.google.android.katniss", now = 1000)
        assertTrue(grace.active(1000))
        assertTrue(grace.active(8999))
        assertFalse(grace.active(9000))
    }

    @Test
    fun otherAppsDoNotPauseIt() {
        val grace = AssistantGrace()
        grace.windowShown("com.netflix.ninja", now = 0)
        grace.windowShown("com.google.android.apps.tv.launcherx", now = 0)
        assertFalse(grace.active(1))
    }
}
