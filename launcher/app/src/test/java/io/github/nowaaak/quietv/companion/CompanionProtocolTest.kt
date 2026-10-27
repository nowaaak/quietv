package io.github.nowaaak.quietv.companion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionProtocolTest {

    private val token = "abcdefghijklmnop"

    @Test
    fun requestWithTheRightTokenIsAuthorized() {
        val line = CompanionProtocol.request(token).lineSequence().first()
        assertTrue(CompanionProtocol.isAuthorized(line, token))
    }

    @Test
    fun wrongMissingOrMalformedTokensAreRejected() {
        assertFalse(CompanionProtocol.isAuthorized("GET /home?token=abcdefghijklmnoX HTTP/1.0", token))
        assertFalse(CompanionProtocol.isAuthorized("GET /home HTTP/1.0", token))
        assertFalse(CompanionProtocol.isAuthorized("POST /home?token=$token HTTP/1.0", token))
        assertFalse(CompanionProtocol.isAuthorized(null, token))
        assertFalse(CompanionProtocol.isAuthorized("GET /home?token= HTTP/1.0", ""))
    }

    @Test
    fun onlyNoContentCountsAsSuccess() {
        assertTrue(CompanionProtocol.isSuccess("HTTP/1.0 204 No Content"))
        assertFalse(CompanionProtocol.isSuccess("HTTP/1.0 403 Forbidden"))
        assertFalse(CompanionProtocol.isSuccess(null))
    }
}
