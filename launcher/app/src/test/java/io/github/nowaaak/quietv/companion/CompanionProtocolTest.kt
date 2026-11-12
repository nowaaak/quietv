package io.github.nowaaak.quietv.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionProtocolTest {

    private val token = "abcdefghijklmnop"

    private fun firstLine(request: String) = request.lineSequence().first()

    @Test
    fun requestWithTheRightTokenIsAuthorized() {
        val request = CompanionProtocol.parse(firstLine(CompanionProtocol.homeRequest(token)))
        assertEquals(CompanionProtocol.HOME, request?.path)
        assertTrue(CompanionProtocol.isAuthorized(request, token))
    }

    @Test
    fun legacyHomeRequestStillParses() {
        val request = CompanionProtocol.parse("GET /home?token=$token HTTP/1.0")
        assertEquals(CompanionProtocol.HOME, request?.path)
        assertTrue(CompanionProtocol.isAuthorized(request, token))
    }

    @Test
    fun wrongMissingOrMalformedTokensAreRejected() {
        fun authorized(line: String?, expected: String = token) =
            CompanionProtocol.isAuthorized(CompanionProtocol.parse(line), expected)
        assertFalse(authorized("GET /home?token=abcdefghijklmnoX HTTP/1.0"))
        assertFalse(authorized("GET /home HTTP/1.0"))
        assertFalse(authorized("POST /home?token=$token HTTP/1.0"))
        assertFalse(authorized(null))
        assertFalse(authorized("GET /home?token= HTTP/1.0", ""))
        assertFalse(authorized("GET /home?token=$token&token=$token HTTP/1.0"))
        assertFalse(authorized("GET /home?token=abc%20def HTTP/1.0", "abc def"))
    }

    @Test
    fun inputRequestRoundTripsTheInputId() {
        val id = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW6"
        val request = CompanionProtocol.parse(firstLine(CompanionProtocol.inputRequest(id, token)))
        assertEquals(CompanionProtocol.INPUT, request?.path)
        assertEquals(id, request?.params?.get("id"))
        assertTrue(CompanionProtocol.isAuthorized(request, token))
    }

    @Test
    fun malformedRequestLinesAreRejected() {
        assertNull(CompanionProtocol.parse("GET /input?id=%zz&token=$token HTTP/1.0"))
        assertNull(CompanionProtocol.parse("GET /input?id&token=$token HTTP/1.0"))
        assertNull(CompanionProtocol.parse("GET /../home?token=$token HTTP/1.0"))
        assertNull(CompanionProtocol.parse("GET /home?token=$token HTTP/2"))
    }

    @Test
    fun inputListRoundTrips() {
        val inputs = listOf(
            RemoteInput("a/HW4", "HDMI 1 · Fire TV", RemoteInput.CONNECTED),
            RemoteInput("a/HW6", "HDMI 3 · PS5", RemoteInput.STANDBY),
        )
        assertEquals(inputs, CompanionProtocol.decodeInputs(CompanionProtocol.encodeInputs(inputs)))
        assertNull(CompanionProtocol.decodeInputs("not json"))
        assertEquals(RemoteInput.UNKNOWN, CompanionProtocol.decodeInputs("""[{"id":"x","label":"y"}]""")?.single()?.state)
    }

    @Test
    fun onlyNoContentCountsAsSuccess() {
        assertTrue(CompanionProtocol.isSuccess("HTTP/1.0 204 No Content"))
        assertFalse(CompanionProtocol.isSuccess("HTTP/1.0 403 Forbidden"))
        assertFalse(CompanionProtocol.isSuccess(null))
        assertEquals(200, CompanionProtocol.statusCode("HTTP/1.0 200 OK"))
    }
}
