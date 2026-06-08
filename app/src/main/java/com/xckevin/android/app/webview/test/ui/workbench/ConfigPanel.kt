package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.model.FeaturePolicy
import com.xckevin.android.app.webview.test.model.MixedContentMode
import com.xckevin.android.app.webview.test.model.UserAgentMode
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebColorMode
import com.xckevin.android.app.webview.test.model.WebPermissionPolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig

@Composable
fun ConfigPanel(
    config: WebTestConfig,
    onConfigChanged: (WebTestConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PanelSection(title = stringResource(R.string.config_section_runtime)) {
                ToggleRow(
                    label = stringResource(R.string.config_javascript),
                    checked = config.javaScriptEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(javaScriptEnabled = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_dom_storage),
                    checked = config.domStorageEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(domStorageEnabled = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_desktop_mode),
                    checked = config.desktopMode,
                    onCheckedChange = { onConfigChanged(config.copy(desktopMode = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_cookies),
                    checked = config.cookiesEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(cookiesEnabled = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_third_party_cookies),
                    checked = config.thirdPartyCookiesEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(thirdPartyCookiesEnabled = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_webview_back_first),
                    checked = config.webViewBackFirst,
                    onCheckedChange = { onConfigChanged(config.copy(webViewBackFirst = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.config_start_fullscreen),
                    checked = config.startFullscreen,
                    onCheckedChange = { onConfigChanged(config.copy(startFullscreen = it)) },
                )
            }
        }

        item {
            PanelSection(title = stringResource(R.string.config_section_user_agent)) {
                EnumChipRow(
                    label = stringResource(R.string.config_user_agent_mode),
                    value = config.userAgentMode,
                    values = UserAgentMode.entries,
                    onValueChanged = { onConfigChanged(config.copy(userAgentMode = it)) },
                )
                OutlinedTextField(
                    value = config.customUserAgent,
                    onValueChange = { onConfigChanged(config.copy(customUserAgent = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    label = { Text(stringResource(R.string.config_custom_ua)) },
                    singleLine = false,
                    enabled = config.userAgentMode == UserAgentMode.CUSTOM,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            PanelSection(title = stringResource(R.string.config_section_network_rendering)) {
                EnumChipRow(
                    label = stringResource(R.string.config_cache_mode),
                    value = config.cacheMode,
                    values = WebCacheMode.entries,
                    onValueChanged = { onConfigChanged(config.copy(cacheMode = it)) },
                )
                EnumChipRow(
                    label = stringResource(R.string.config_mixed_content),
                    value = config.mixedContentMode,
                    values = MixedContentMode.entries,
                    onValueChanged = { onConfigChanged(config.copy(mixedContentMode = it)) },
                )
                EnumChipRow(
                    label = stringResource(R.string.config_color_mode),
                    value = config.colorMode,
                    values = WebColorMode.entries,
                    onValueChanged = { onConfigChanged(config.copy(colorMode = it)) },
                )
            }
        }

        item {
            PanelSection(title = stringResource(R.string.config_section_permissions)) {
                EnumChipRow(
                    label = stringResource(R.string.config_geolocation),
                    value = config.geolocationPolicy,
                    values = WebPermissionPolicy.entries,
                    onValueChanged = { onConfigChanged(config.copy(geolocationPolicy = it)) },
                )
                EnumChipRow(
                    label = stringResource(R.string.config_file_chooser),
                    value = config.fileChooserPolicy,
                    values = FeaturePolicy.entries,
                    onValueChanged = { onConfigChanged(config.copy(fileChooserPolicy = it)) },
                )
                EnumChipRow(
                    label = stringResource(R.string.config_camera),
                    value = config.cameraPolicy,
                    values = WebPermissionPolicy.entries,
                    onValueChanged = { onConfigChanged(config.copy(cameraPolicy = it)) },
                )
                EnumChipRow(
                    label = stringResource(R.string.config_microphone),
                    value = config.microphonePolicy,
                    values = WebPermissionPolicy.entries,
                    onValueChanged = { onConfigChanged(config.copy(microphonePolicy = it)) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PanelRow(label = label) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumChipRow(
    label: String,
    value: T,
    values: List<T>,
    onValueChanged: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { option ->
                FilterChip(
                    selected = option == value,
                    onClick = { onValueChanged(option) },
                    label = {
                        Text(
                            option.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Enum<*>.displayName(): String =
    stringResource(
        when (this) {
            UserAgentMode.DEFAULT, WebCacheMode.DEFAULT -> R.string.enum_default
            UserAgentMode.CUSTOM -> R.string.enum_custom
            UserAgentMode.DESKTOP -> R.string.enum_desktop
            WebCacheMode.NO_CACHE -> R.string.enum_no_cache
            WebCacheMode.CLEAR_BEFORE_LOAD -> R.string.enum_clear_before_load
            MixedContentMode.ALLOW, WebPermissionPolicy.ALLOW, FeaturePolicy.ALLOW -> R.string.enum_allow
            MixedContentMode.BLOCK -> R.string.enum_block
            WebColorMode.FOLLOW_SYSTEM -> R.string.enum_follow_system
            WebColorMode.FORCE_LIGHT -> R.string.enum_force_light
            WebColorMode.FORCE_DARK -> R.string.enum_force_dark
            WebPermissionPolicy.DENY, FeaturePolicy.DENY -> R.string.enum_deny
            WebPermissionPolicy.ASK_EVERY_TIME -> R.string.enum_ask_every_time
            else -> R.string.enum_default
        }
    )
