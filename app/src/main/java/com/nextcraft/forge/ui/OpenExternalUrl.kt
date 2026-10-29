package com.nextcraft.forge.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nextcraft.forge.R
import kotlinx.coroutines.delay

/**
 * Opens [url] in the browser. A failed launch (no handler, or a non-Activity context that needs a new task) used to be
 * swallowed, so the sign-in button looked dead; now it retries in a new task and, if that also fails, copies the link
 * and tells the user.
 */
internal fun openUrlOrCopy(context: Context, url: String, copyToClipboard: (String) -> Unit) {
    val view = Intent(Intent.ACTION_VIEW, url.toUri())
    val opened = runCatching { context.startActivity(view) }
        .recoverCatching { context.startActivity(view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Log.w("OpenExternalUrl", "Could not open the browser", it) }
        .isSuccess
    if (!opened) {
        copyToClipboard(url)
        Toast.makeText(context, R.string.agent_open_link_failed_copied, Toast.LENGTH_LONG).show()
    }
}

private val autoOpenedLinks = HashSet<String>()
private var lastAutoSubmittedCode: String? = null

/** Google OAuth authorization codes look like `4/0A...`; anything else on the clipboard is ignored. */
private val GOOGLE_AUTH_CODE = Regex("^4/[A-Za-z0-9_.\\-]{20,200}$")

internal fun looksLikeGoogleAuthCode(text: String): Boolean = GOOGLE_AUTH_CODE.matches(text.trim())

/**
 * Opens the sign-in page by itself the first time [url] shows up, so the user does not have to find the button.
 * The same link is never opened twice, even when the screen is recomposed or revisited.
 */
@Composable
internal fun AutoOpenSignInLink(url: String?) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(url) {
        if (url != null && synchronized(autoOpenedLinks) { autoOpenedLinks.add(url) }) {
            openUrlOrCopy(context, url) { clipboard.setText(AnnotatedString(it)) }
        }
    }
}

/**
 * Back from the browser with a Google authorization code copied (the callback page offers a copy button): submit it
 * instead of making the user paste it. Only a strict `4/...` code is used, and a code is never sent twice.
 */
@Composable
internal fun AutoSubmitClipboardCode(awaitingCode: Boolean, onCode: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    LaunchedEffect(resumes, awaitingCode) {
        if (!awaitingCode) return@LaunchedEffect
        // The clipboard is only readable once the window has focus again, which can lag behind ON_RESUME.
        for (waitMs in longArrayOf(0, 300, 700, 1_500)) {
            delay(waitMs)
            val text = clipboard.getText()?.text?.trim().orEmpty()
            if (looksLikeGoogleAuthCode(text) && text != lastAutoSubmittedCode) {
                lastAutoSubmittedCode = text
                onCode(text)
                return@LaunchedEffect
            }
        }
    }
}

/** Backs the "open browser" button: uses the known link, asks [resolve] for one that was not picked up yet, or says it is not ready. */
internal fun openSignInLink(context: Context, known: String?, resolve: () -> String?, copyToClipboard: (String) -> Unit) {
    val url = known ?: resolve()
    if (url == null) {
        Toast.makeText(context, R.string.agent_sign_in_link_not_ready, Toast.LENGTH_LONG).show()
        return
    }
    openUrlOrCopy(context, url, copyToClipboard)
}
