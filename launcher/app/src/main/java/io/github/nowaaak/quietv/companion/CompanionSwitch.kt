package io.github.nowaaak.quietv.companion

import android.os.Handler
import android.os.Looper
import io.github.nowaaak.quietv.config.CompanionConfig
import java.io.BufferedReader
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

object CompanionSwitch {

    private const val TIMEOUT_MS = 2000
    private const val MAX_BODY = 64 * 1024

    sealed interface InputsResult {
        data class Inputs(val inputs: List<RemoteInput>) : InputsResult
        data object Unsupported : InputsResult
        data object Unreachable : InputsResult
    }

    fun showHome(config: CompanionConfig, onFailure: () -> Unit) =
        send(config, CompanionProtocol.homeRequest(config.token), onFailure)

    fun openInput(config: CompanionConfig, id: String, onFailure: () -> Unit) =
        send(config, CompanionProtocol.inputRequest(id, config.token), onFailure)

    fun fetchInputs(config: CompanionConfig, onResult: (InputsResult) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        Thread({
            val result = try {
                exchange(config, CompanionProtocol.inputsRequest(config.token)) { status, reader ->
                    if (CompanionProtocol.statusCode(status) != 200) return@exchange InputsResult.Unsupported
                    while (!reader.readLine().isNullOrEmpty()) Unit
                    val body = CharArray(MAX_BODY)
                    var length = 0
                    while (length < MAX_BODY) {
                        val read = reader.read(body, length, MAX_BODY - length)
                        if (read < 0) break
                        length += read
                    }
                    CompanionProtocol.decodeInputs(String(body, 0, length))
                        ?.let { InputsResult.Inputs(it) } ?: InputsResult.Unsupported
                }
            } catch (e: IOException) {
                InputsResult.Unreachable
            }
            main.post { onResult(result) }
        }, "companion-inputs").start()
    }

    private fun send(config: CompanionConfig, request: String, onFailure: () -> Unit) {
        val main = Handler(Looper.getMainLooper())
        Thread({
            val ok = try {
                exchange(config, request) { status, _ -> CompanionProtocol.isSuccess(status) }
            } catch (e: IOException) {
                false
            }
            if (!ok) main.post(onFailure)
        }, "companion-send").start()
    }

    private fun <T> exchange(config: CompanionConfig, request: String, read: (String?, BufferedReader) -> T): T =
        Socket().use { socket ->
            socket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            read(reader.readLine(), reader)
        }
}
