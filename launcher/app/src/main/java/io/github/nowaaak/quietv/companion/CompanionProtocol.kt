package io.github.nowaaak.quietv.companion

import java.security.MessageDigest

object CompanionProtocol {

    private val REQUEST = Regex("^GET /home\\?token=([A-Za-z0-9_-]+) HTTP/1\\.[01]$")

    fun request(token: String): String = "GET /home?token=$token HTTP/1.0\r\n\r\n"

    fun isAuthorized(requestLine: String?, token: String): Boolean {
        if (requestLine == null || token.isEmpty()) return false
        val sent = REQUEST.matchEntire(requestLine.trim())?.groupValues?.get(1) ?: return false
        return MessageDigest.isEqual(sent.toByteArray(Charsets.US_ASCII), token.toByteArray(Charsets.US_ASCII))
    }

    fun isSuccess(statusLine: String?): Boolean = statusLine?.contains(" 204") == true
}
