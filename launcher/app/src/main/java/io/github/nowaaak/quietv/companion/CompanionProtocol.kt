package io.github.nowaaak.quietv.companion

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

data class CompanionRequest(val path: String, val params: Map<String, String>)

data class RemoteInput(val id: String, val label: String, val state: String) {
    companion object {
        const val CONNECTED = "connected"
        const val DISCONNECTED = "disconnected"
        const val STANDBY = "standby"
        const val UNKNOWN = "unknown"
    }
}

object CompanionProtocol {

    const val HOME = "/home"
    const val INPUTS = "/inputs"
    const val INPUT = "/input"

    private val REQUEST_LINE = Regex("^GET (/[a-z]+)(?:\\?([^ ]*))? HTTP/1\\.[01]$")
    private val TOKEN = Regex("^[A-Za-z0-9_-]+$")

    fun homeRequest(token: String): String = request(HOME, mapOf("token" to token))

    fun inputsRequest(token: String): String = request(INPUTS, mapOf("token" to token))

    fun inputRequest(id: String, token: String): String = request(INPUT, mapOf("id" to id, "token" to token))

    private fun request(path: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return "GET $path?$query HTTP/1.0\r\n\r\n"
    }

    fun parse(requestLine: String?): CompanionRequest? {
        val match = REQUEST_LINE.matchEntire(requestLine?.trim() ?: return null) ?: return null
        val params = mutableMapOf<String, String>()
        for (pair in match.groupValues[2].split('&')) {
            if (pair.isEmpty()) continue
            val parts = pair.split('=', limit = 2)
            if (parts.size != 2) return null
            val value = try {
                URLDecoder.decode(parts[1], "UTF-8")
            } catch (e: IllegalArgumentException) {
                return null
            }
            if (params.put(parts[0], value) != null) return null
        }
        return CompanionRequest(match.groupValues[1], params)
    }

    fun isAuthorized(request: CompanionRequest?, token: String): Boolean {
        if (request == null || token.isEmpty()) return false
        val sent = request.params["token"]?.takeIf { TOKEN.matches(it) } ?: return false
        return MessageDigest.isEqual(sent.toByteArray(Charsets.US_ASCII), token.toByteArray(Charsets.US_ASCII))
    }

    fun statusCode(statusLine: String?): Int? =
        statusLine?.trim()?.split(' ')?.getOrNull(1)?.toIntOrNull()

    fun isSuccess(statusLine: String?): Boolean = statusCode(statusLine) == 204

    fun encodeInputs(inputs: List<RemoteInput>): String = JSONArray().also { array ->
        inputs.forEach { array.put(JSONObject().put("id", it.id).put("label", it.label).put("state", it.state)) }
    }.toString()

    fun decodeInputs(body: String): List<RemoteInput>? = try {
        val array = JSONArray(body)
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            RemoteInput(item.getString("id"), item.getString("label"), item.optString("state", RemoteInput.UNKNOWN))
        }
    } catch (e: JSONException) {
        null
    }
}
