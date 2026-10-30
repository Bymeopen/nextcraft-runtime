package com.nextcraft.forge.ui

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nextcraft.forge.CrashReporter
import com.nextcraft.forge.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CrashReportPrompt() {
    val hostContext = LocalContext.current
    var showPrompt by remember { mutableStateOf(false) }
    // hasUnseen reads the disk: keep it out of composition.
    LaunchedEffect(Unit) {
        showPrompt = withContext(Dispatchers.IO) { CrashReporter.hasUnseen(hostContext) }
    }
    if (!showPrompt) return

    fun acknowledgeAndClose() {
        CrashReporter.markSeen(hostContext)
        showPrompt = false
    }

    ForgeAlertDialog(
        onDismissRequest = ::acknowledgeAndClose,
        title = { Text(stringResource(R.string.crash_prompt_title)) },
        text = { Text(stringResource(R.string.crash_prompt_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    CrashReporter.shareIntent(hostContext)?.let { chooser ->
                        hostContext.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    acknowledgeAndClose()
                },
            ) {
                Text(stringResource(R.string.crash_prompt_share))
            }
        },
        dismissButton = {
            TextButton(onClick = ::acknowledgeAndClose) {
                Text(stringResource(R.string.crash_prompt_later))
            }
        },
    )
}
