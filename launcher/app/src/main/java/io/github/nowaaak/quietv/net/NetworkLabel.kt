package io.github.nowaaak.quietv.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import io.github.nowaaak.quietv.R

data class NetworkStatus(val label: String, val online: Boolean)

object NetworkLabel {

    fun read(context: Context): NetworkStatus {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: return NetworkStatus(context.getString(R.string.offline), false)
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkStatus(context.getString(R.string.ethernet), true)
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkStatus(ssid(context) ?: context.getString(R.string.wifi), true)
        }
        return NetworkStatus(context.getString(R.string.wifi), true)
    }

    @Suppress("DEPRECATION")
    private fun ssid(context: Context): String? {
        val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val raw = try {
            wm.connectionInfo?.ssid
        } catch (e: SecurityException) {
            null
        } ?: return null
        val ssid = raw.replace("\"", "").trim()
        return ssid.takeIf { it.isNotEmpty() && !it.contains("unknown") && it != "0x" }
    }
}
