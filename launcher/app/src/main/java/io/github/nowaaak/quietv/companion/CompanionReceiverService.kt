package io.github.nowaaak.quietv.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import io.github.nowaaak.quietv.MainActivity
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.CompanionMode
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.inputs.InputCatalog
import io.github.nowaaak.quietv.inputs.InputEntry
import io.github.nowaaak.quietv.inputs.InputsMenu
import java.io.IOException
import java.net.ServerSocket

class CompanionReceiverService : Service() {

    @Volatile
    private var server: ServerSocket? = null
    private var worker: Thread? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.companion_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_livetv)
            .setContentTitle(getString(R.string.companion_channel))
            .build()
        startForeground(NOTIFICATION_ID, notification)
        val config = ConfigStore(this).load().companion
        worker = Thread({ serve(config.port, config.token) }, "companion-receive").also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun serve(port: Int, token: String) {
        try {
            ServerSocket(port).use { socket ->
                server = socket
                while (!socket.isClosed) {
                    try {
                        socket.accept().use { client ->
                            client.soTimeout = TIMEOUT_MS
                            val local = client.inetAddress.isSiteLocalAddress || client.inetAddress.isLoopbackAddress
                            val line = client.getInputStream().bufferedReader(Charsets.US_ASCII).readLine()
                            val request = CompanionProtocol.parse(line)
                            val allowed = local && CompanionProtocol.isAuthorized(request, token)
                            val reply = if (allowed && request != null) {
                                handle(request)
                            } else {
                                Log.w(TAG, "rejected request from ${client.inetAddress.hostAddress}")
                                Reply(FORBIDDEN)
                            }
                            client.getOutputStream().apply {
                                write(reply.bytes())
                                flush()
                            }
                            reply.after?.let { main.post(it) }
                        }
                    } catch (e: IOException) {
                        if (!socket.isClosed) Log.w(TAG, "request failed", e)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "cannot listen on port $port", e)
        }
    }

    private fun handle(request: CompanionRequest): Reply = when (request.path) {
        CompanionProtocol.HOME -> Reply(NO_CONTENT, after = ::showHome)
        CompanionProtocol.INPUTS -> Reply(OK, CompanionProtocol.encodeInputs(InputCatalog.passthrough(this).map { it.toRemote() }))
        CompanionProtocol.INPUT -> {
            val wanted = request.params["id"]
            val input = InputCatalog.passthrough(this).firstOrNull { it.id == wanted || it.portId == wanted }
            if (input == null) Reply(NOT_FOUND) else Reply(NO_CONTENT, after = { openInput(input.id) })
        }
        else -> Reply(NOT_FOUND)
    }

    private fun InputEntry.toRemote() = RemoteInput(
        id,
        label,
        when (state) {
            TvInputManager.INPUT_STATE_CONNECTED -> RemoteInput.CONNECTED
            TvInputManager.INPUT_STATE_DISCONNECTED -> RemoteInput.DISCONNECTED
            TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> if (hasDevice) RemoteInput.STANDBY else RemoteInput.UNKNOWN
            else -> RemoteInput.UNKNOWN
        },
    )

    private fun openInput(id: String) {
        val store = ConfigStore(this)
        store.lastInput = id
        InputsMenu.openInput(this, id, store.load().liveTvPackage)
    }

    private class Reply(val status: String, val body: String? = null, val after: (() -> Unit)? = null) {
        fun bytes(): ByteArray {
            val content = body?.toByteArray(Charsets.UTF_8)
            val head = buildString {
                append("HTTP/1.0 ").append(status).append("\r\n")
                if (content != null) {
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Content-Length: ").append(content.size).append("\r\n")
                }
                append("\r\n")
            }.toByteArray(Charsets.US_ASCII)
            return if (content == null) head else head + content
        }
    }

    private fun showHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun onDestroy() {
        try {
            server?.close()
        } catch (e: IOException) {
            Log.w(TAG, "close failed", e)
        }
        worker?.interrupt()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CompanionReceiver"
        private const val CHANNEL = "companion"
        private const val NOTIFICATION_ID = 1
        private const val TIMEOUT_MS = 2000
        private const val OK = "200 OK"
        private const val NO_CONTENT = "204 No Content"
        private const val FORBIDDEN = "403 Forbidden"
        private const val NOT_FOUND = "404 Not Found"

        fun sync(context: Context, config: LauncherConfig) {
            val intent = Intent(context, CompanionReceiverService::class.java)
            if (config.companion.mode == CompanionMode.RECEIVE) {
                try {
                    context.startForegroundService(intent)
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "cannot start the companion receiver now", e)
                }
            } else {
                context.stopService(intent)
            }
        }
    }
}
