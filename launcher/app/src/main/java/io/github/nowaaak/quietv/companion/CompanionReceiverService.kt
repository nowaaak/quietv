package io.github.nowaaak.quietv.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.github.nowaaak.quietv.MainActivity
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.CompanionMode
import io.github.nowaaak.quietv.config.ConfigStore
import io.github.nowaaak.quietv.config.LauncherConfig
import java.io.IOException
import java.net.ServerSocket

class CompanionReceiverService : Service() {

    @Volatile
    private var server: ServerSocket? = null
    private var worker: Thread? = null

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
                            val allowed = local && CompanionProtocol.isAuthorized(line, token)
                            val reply = if (allowed) "HTTP/1.0 204 No Content\r\n\r\n" else "HTTP/1.0 403 Forbidden\r\n\r\n"
                            client.getOutputStream().apply {
                                write(reply.toByteArray(Charsets.US_ASCII))
                                flush()
                            }
                            if (allowed) showHome() else Log.w(TAG, "rejected request from ${client.inetAddress.hostAddress}")
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
