package com.xckevin.android.app.webview.test.ui.workbench

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UrlBar(
    urlInput: String,
    urlError: String?,
    isLoading: Boolean,
    loadProgress: Int,
    isFullscreen: Boolean,
    onUrlInputChanged: (String) -> Unit,
    onLoad: () -> Unit,
    onScan: () -> Unit,
    onOpenLocalHtml: () -> Unit = {},
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlInputChanged,
                modifier = Modifier.weight(1f),
                label = { Text("URL") },
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onLoad() }),
            )
            Button(
                onClick = onLoad,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Load")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val text = clipboard.getClipEntry()
                            ?.clipData
                            ?.takeIf { clipData ->
                                clipData.description.hasMimeType(MIMETYPE_TEXT_PLAIN) ||
                                    clipData.description.hasMimeType("text/*")
                            }
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.text
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                        if (text != null) {
                            onUrlInputChanged(text)
                        }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Paste" },
            ) {
                Text("Paste")
            }
            OutlinedButton(
                onClick = onScan,
                modifier = Modifier.semantics { contentDescription = "Scan" },
            ) {
                Text("Scan")
            }
            OutlinedButton(
                onClick = onOpenLocalHtml,
                modifier = Modifier.semantics { contentDescription = "Open local HTML" },
            ) {
                Text("Open local HTML")
            }
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.semantics { contentDescription = "Refresh" },
            ) {
                Text("Refresh")
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.semantics { contentDescription = "Settings" },
            ) {
                Text("Settings")
            }
            OutlinedButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.semantics { contentDescription = "Fullscreen" },
            ) {
                Text(if (isFullscreen) "Exit fullscreen" else "Fullscreen")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
