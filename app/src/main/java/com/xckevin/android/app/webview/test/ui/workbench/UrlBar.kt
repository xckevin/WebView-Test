package com.xckevin.android.app.webview.test.ui.workbench

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import kotlinx.coroutines.launch

@Composable
fun UrlBar(
    urlInput: String,
    urlError: String?,
    isLoading: Boolean,
    loadProgress: Int,
    isFullscreen: Boolean,
    onOpenTools: () -> Unit,
    onUrlInputChanged: (String) -> Unit,
    onLoad: () -> Unit,
    onScan: () -> Unit,
    onOpenLocalHtml: () -> Unit = {},
    onOpenPermissionFixture: () -> Unit = {},
    onOpenLocalFixture: () -> Unit = {},
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var isOverflowOpen by remember { mutableStateOf(false) }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val pasteDescription = stringResource(R.string.action_paste)
    val scanDescription = stringResource(R.string.action_scan)
    val openLocalHtmlDescription = stringResource(R.string.action_open_local_html)
    val permissionFixtureDescription = stringResource(R.string.action_open_permission_fixture)
    val localFixtureDescription = stringResource(R.string.action_open_local_fixture)
    val refreshDescription = stringResource(R.string.action_refresh)
    val settingsDescription = stringResource(R.string.screen_settings)
    val fullscreenDescription = stringResource(
        if (isFullscreen) R.string.action_exit_fullscreen else R.string.action_fullscreen
    )
    val pasteFromClipboard = {
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
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenTools) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.action_open_tools),
                    tint = iconTint,
                )
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { error -> { Text(error.localizedUrlError()) } },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onLoad) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.action_load),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onLoad() }),
            )

            Box {
                IconButton(onClick = { isOverflowOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = iconTint,
                    )
                }

                DropdownMenu(
                    expanded = isOverflowOpen,
                    onDismissRequest = { isOverflowOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(pasteDescription) },
                        onClick = {
                            isOverflowOpen = false
                            pasteFromClipboard()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(scanDescription) },
                        onClick = {
                            isOverflowOpen = false
                            onScan()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(openLocalHtmlDescription) },
                        onClick = {
                            isOverflowOpen = false
                            onOpenLocalHtml()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(permissionFixtureDescription) },
                        onClick = {
                            isOverflowOpen = false
                            onOpenPermissionFixture()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Science, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(localFixtureDescription) },
                        onClick = {
                            isOverflowOpen = false
                            onOpenLocalFixture()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Science, contentDescription = null)
                        },
                    )
//                    DropdownMenuItem(
//                        text = { Text(refreshDescription) },
//                        onClick = {
//                            isOverflowOpen = false
//                            onRefresh()
//                        },
//                        leadingIcon = {
//                            Icon(Icons.Outlined.Refresh, contentDescription = null)
//                        },
//                    )
                    DropdownMenuItem(
                        text = { Text(settingsDescription) },
                        onClick = {
                            isOverflowOpen = false
                            onOpenSettings()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                        },
                    )
//                    DropdownMenuItem(
//                        text = { Text(fullscreenDescription) },
//                        onClick = {
//                            isOverflowOpen = false
//                            onToggleFullscreen()
//                        },
//                        leadingIcon = {
//                            Icon(
//                                if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
//                                contentDescription = null,
//                            )
//                        },
//                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(top = 2.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun String.localizedUrlError(): String =
    when (this) {
        "Enter a valid http or https URL" -> stringResource(R.string.error_invalid_http_url)
        else -> this
    }
