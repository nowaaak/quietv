package io.github.nowaaak.quietv.companion

import android.os.Handler
import android.os.Looper
import io.github.nowaaak.quietv.config.CompanionConfig
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

object CompanionSwitch {

    private const val TIMEOUT_MS = 2000

    fun send(config: CompanionConfig, onFailure: () -> Unit) {
        val main = Handler(Looper.getMainLooper())
        Thread({
            val ok = try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                    socket.soTimeout = TIMEOUT_MS
                    socket.getOutputStream().write(CompanionProtocol.request(config.token).toByteArray(Charsets.US_ASCII))
                    CompanionProtocol.isSuccess(socket.getInputStream().bufferedReader(Charsets.US_ASCII).readLine())
                }
            } catch (e: IOException) {
                false
            }
            if (!ok) main.post(onFailure)
        }, "companion-send").start()
    }
}
