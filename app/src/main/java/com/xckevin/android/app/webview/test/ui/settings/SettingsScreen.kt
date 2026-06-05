package com.xckevin.android.app.webview.test.ui.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.AppContainer
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.ui.cases.CaseImportExportActions
import com.xckevin.android.app.webview.test.ui.common.AppScaffold

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    settingsRepository = container.settingsStore,
                    historyRepository = container.historyRepository,
                ) as T
            }
        }
    }
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmationAction by remember { mutableStateOf<SettingsAction?>(null) }

    confirmationAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmationAction = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmationAction = null
                        when (action) {
                            SettingsAction.ResetHistory -> viewModel.resetHistory()
                            SettingsAction.ResetWebDefaults -> viewModel.resetWebDefaults()
                            SettingsAction.ClearCookies -> {
                                viewModel.clearCookies()
                                clearCookies { removed ->
                                    viewModel.setStatusMessage("Cookies cleared: $removed")
                                }
                            }
                            SettingsAction.ClearWebViewCache -> {
                                viewModel.clearWebViewCache()
                                WebStorage.getInstance().deleteAllData()
                                WebView(context).apply {
                                    clearCache(true)
                                    destroy()
                                }
                                viewModel.setStatusMessage("WebView cache cleared")
                            }
                        }
                    },
                ) {
                    Text(action.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationAction = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.screen_settings),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(onClick = onBack) {
                    Text(text = stringResource(R.string.action_back))
                }
            }

            SettingsSection(title = "WebView debugging") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Enable WebView debugging", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Current status: ${if (state.webContentsDebuggingEnabled) "enabled" else "disabled"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = state.webContentsDebuggingEnabled,
                        onCheckedChange = viewModel::setWebContentsDebuggingEnabled,
                    )
                }
                Text(
                    text = "Connect this device, open chrome://inspect in Chrome, then inspect this app's WebView under Remote Target.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SettingsSection(title = "Reset actions") {
                ResetButton(
                    text = "Reset history",
                    onClick = { confirmationAction = SettingsAction.ResetHistory },
                )
                ResetButton(
                    text = "Reset WebView defaults",
                    onClick = { confirmationAction = SettingsAction.ResetWebDefaults },
                )
                ResetButton(
                    text = "Clear cookies",
                    onClick = { confirmationAction = SettingsAction.ClearCookies },
                )
                ResetButton(
                    text = "Clear WebView cache",
                    onClick = { confirmationAction = SettingsAction.ClearWebViewCache },
                )
                Text(
                    text = "Debug logs are session scoped. Clear them from the active Workbench Debug panel.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SettingsSection(title = "Import and export") {
                CaseImportExportActions(
                    testCaseRepository = container.testCaseRepository,
                    contentResolver = context.contentResolver,
                    onStatusMessage = viewModel::setStatusMessage,
                )
            }

            state.statusMessage?.let { message ->
                HorizontalDivider()
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun ResetButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}

private fun clearCookies(onComplete: (Boolean) -> Unit) {
    CookieManager.getInstance().removeAllCookies { removed ->
        CookieManager.getInstance().flush()
        onComplete(removed)
    }
}

private enum class SettingsAction(
    val title: String,
    val message: String,
    val confirmLabel: String,
) {
    ResetHistory(
        title = "Reset history?",
        message = "This permanently clears the browsing history stored by this app.",
        confirmLabel = "Reset",
    ),
    ResetWebDefaults(
        title = "Reset WebView defaults?",
        message = "This restores the default WebView test configuration used for new sessions.",
        confirmLabel = "Reset",
    ),
    ClearCookies(
        title = "Clear cookies?",
        message = "This removes cookies from the WebView cookie store.",
        confirmLabel = "Clear",
    ),
    ClearWebViewCache(
        title = "Clear WebView cache?",
        message = "This clears WebView cache and web storage data for this app process.",
        confirmLabel = "Clear",
    ),
}
