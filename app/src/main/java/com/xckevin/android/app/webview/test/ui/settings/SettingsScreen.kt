package com.xckevin.android.app.webview.test.ui.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
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
    val cookiesClearedStatus = stringResource(R.string.settings_cookies_cleared_status)
    val cacheClearedStatus = stringResource(R.string.settings_cache_cleared_status)

    confirmationAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmationAction = null },
            title = { Text(stringResource(action.titleRes)) },
            text = { Text(stringResource(action.messageRes)) },
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
                                    viewModel.setStatusMessage(cookiesClearedStatus.format(removed))
                                }
                            }
                            SettingsAction.ClearWebViewCache -> {
                                viewModel.clearWebViewCache()
                                WebStorage.getInstance().deleteAllData()
                                WebView(context).apply {
                                    clearCache(true)
                                    destroy()
                                }
                                viewModel.setStatusMessage(cacheClearedStatus)
                            }
                        }
                    },
                ) {
                    Text(stringResource(action.confirmLabelRes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationAction = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_webview_debugging), icon = Icons.Outlined.BugReport) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(R.string.settings_enable_webview_debugging), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (state.webContentsDebuggingEnabled) {
                                stringResource(R.string.settings_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.webContentsDebuggingEnabled,
                        onCheckedChange = viewModel::setWebContentsDebuggingEnabled,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_debugging_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_reset_actions), icon = Icons.Outlined.RestartAlt) {
                ResetButton(
                    text = stringResource(R.string.settings_reset_history),
                    icon = Icons.Outlined.History,
                    onClick = { confirmationAction = SettingsAction.ResetHistory },
                )
                ResetButton(
                    text = stringResource(R.string.settings_reset_webview_defaults),
                    icon = Icons.Outlined.SettingsBackupRestore,
                    onClick = { confirmationAction = SettingsAction.ResetWebDefaults },
                )
                ResetButton(
                    text = stringResource(R.string.settings_clear_cookies),
                    icon = Icons.Outlined.Cookie,
                    onClick = { confirmationAction = SettingsAction.ClearCookies },
                )
                ResetButton(
                    text = stringResource(R.string.settings_clear_webview_cache),
                    icon = Icons.Outlined.CleaningServices,
                    onClick = { confirmationAction = SettingsAction.ClearWebViewCache },
                )
                Text(
                    text = stringResource(R.string.settings_debug_logs_session_scoped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.statusMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = message.localizedSettingsStatus(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun ResetButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Spacer(Modifier.width(4.dp))
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
    @get:StringRes val titleRes: Int,
    @get:StringRes val messageRes: Int,
    @get:StringRes val confirmLabelRes: Int,
) {
    ResetHistory(
        titleRes = R.string.settings_reset_history_title,
        messageRes = R.string.settings_reset_history_message,
        confirmLabelRes = R.string.settings_confirm_reset,
    ),
    ResetWebDefaults(
        titleRes = R.string.settings_reset_webview_defaults_title,
        messageRes = R.string.settings_reset_webview_defaults_message,
        confirmLabelRes = R.string.settings_confirm_reset,
    ),
    ClearCookies(
        titleRes = R.string.settings_clear_cookies_title,
        messageRes = R.string.settings_clear_cookies_message,
        confirmLabelRes = R.string.settings_confirm_clear,
    ),
    ClearWebViewCache(
        titleRes = R.string.settings_clear_webview_cache_title,
        messageRes = R.string.settings_clear_webview_cache_message,
        confirmLabelRes = R.string.settings_confirm_clear,
    ),
}

@Composable
private fun String.localizedSettingsStatus(): String =
    when (this) {
        "WebView debugging enabled" -> stringResource(R.string.settings_debugging_enabled_status)
        "WebView debugging disabled" -> stringResource(R.string.settings_debugging_disabled_status)
        "History cleared" -> stringResource(R.string.settings_history_cleared_status)
        "Default WebView config restored" -> stringResource(R.string.settings_default_config_restored_status)
        "Clearing cookies" -> stringResource(R.string.settings_clearing_cookies_status)
        "Clearing WebView cache" -> stringResource(R.string.settings_clearing_cache_status)
        "WebView cache cleared" -> stringResource(R.string.settings_cache_cleared_status)
        else -> this
    }
