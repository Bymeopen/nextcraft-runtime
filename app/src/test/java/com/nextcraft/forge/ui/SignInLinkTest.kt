package com.nextcraft.forge.ui

import com.nextcraft.forge.runtime.isGoogleSignInUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInLinkTest {
    private val googleUrl =
        "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=abc.apps.googleusercontent.com" +
            "&code_challenge=xyz&code_challenge_method=S256&redirect_uri=https%3A%2F%2Fantigravity.google%2Foauth-callback"

    @Test
    fun googleSignInUrl_acceptsOnlyGoogleOAuthEndpoint() {
        assertTrue(isGoogleSignInUrl(googleUrl))
        assertFalse(isGoogleSignInUrl(googleUrl.replace("accounts.google.com", "accounts.google.com.evil.example")))
        assertFalse(isGoogleSignInUrl(googleUrl.replace("https://", "http://")))
        assertFalse(isGoogleSignInUrl("https://accounts.google.com/o/oauth2/auth?state=1"))
        assertFalse(isGoogleSignInUrl("not a url"))
    }

    @Test
    fun authCode_matchesOnlyGoogleCodeShape() {
        assertTrue(looksLikeGoogleAuthCode("4/0AVMBsJh_example-Code.1234567890abcdefghij"))
        assertTrue(looksLikeGoogleAuthCode("  4/0AVMBsJh_example-Code.1234567890abcdefghij \n"))
        assertFalse(looksLikeGoogleAuthCode("4/short"))
        assertFalse(looksLikeGoogleAuthCode("hello world, this is just some copied text"))
        assertFalse(looksLikeGoogleAuthCode("https://antigravity.google/oauth-callback?code=4/0AVMBsJh_example"))
        assertFalse(looksLikeGoogleAuthCode(""))
    }
}
