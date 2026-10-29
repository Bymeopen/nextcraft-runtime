package com.nextcraft.forge.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.nextcraft.forge.R

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
