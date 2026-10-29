package com.nextcraft.forge.runtime

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The fixture is the raw PTY output of `agy` 1.4.10 captured on a device (80x24 pty) up to the sign-in link; its OAuth state is long dead. */
class AntigravityLoginScreenTest {
    private val screen = checkNotNull(javaClass.getResourceAsStream("/agy-1.4.10-login-screen.bin")) { "fixture missing" }
        .use { it.readBytes().decodeToString() }

    @Test
    fun loginMethodPicker_isRecognised() {
        val text = stripAnsiSequences(screen)
        assertTrue(text.contains("Select login method", ignoreCase = true))
        assertTrue(text.contains("> 1. Google OAuth", ignoreCase = true))
    }

    @Test
    fun wrappedSignInLink_isExtractedFromTheScreen() {
        val url = extractGoogleOAuthUrl(stripAnsiSequences(screen))
        assertNotNull(url)
        assertTrue(isGoogleSignInUrl(url!!))
        assertTrue("state=" in url && "redirect_uri=https%3A%2F%2Fantigravity.google%2Foauth-callback" in url)
    }
}
