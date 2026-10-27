package io.github.nowaaak.quietv.firetv

class AssistantGrace(private val graceMs: Long = GRACE_MS) {

    private var until = 0L

    fun windowShown(pkg: String, now: Long) {
        if (pkg in ASSISTANTS) until = now + graceMs
    }

    fun active(now: Long): Boolean = now < until

    companion object {
        const val GRACE_MS = 8000L
        val ASSISTANTS = setOf("com.google.android.katniss")
    }
}
