package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.model.FeaturePolicy
import com.xckevin.android.app.webview.test.model.MixedContentMode
import com.xckevin.android.app.webview.test.model.UserAgentMode
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebColorMode
import com.xckevin.android.app.webview.test.model.WebPermissionPolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig
import java.util.Locale

@Composable
fun ConfigPanel(
    config: WebTestConfig,
    onConfigChanged: (WebTestConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PanelHeader("Runtime")
            ToggleRow(
                label = "JavaScript",
                checked = config.javaScriptEnabled,
                onCheckedChange = { onConfigChanged(config.copy(javaScriptEnabled = it)) },
            )
            ToggleRow(
                label = "DOM Storage",
                checked = config.domStorageEnabled,
                onCheckedChange = { onConfigChanged(config.copy(domStorageEnabled = it)) },
            )
            ToggleRow(
                label = "Desktop mode",
                checked = config.desktopMode,
                onCheckedChange = { onConfigChanged(config.copy(desktopMode = it)) },
            )
            ToggleRow(
                label = "Cookies",
                checked = config.cookiesEnabled,
                onCheckedChange = { onConfigChanged(config.copy(cookiesEnabled = it)) },
            )
            ToggleRow(
                label = "Third-party cookies",
                checked = config.thirdPartyCookiesEnabled,
                onCheckedChange = { onConfigChanged(config.copy(thirdPartyCookiesEnabled = it)) },
            )
            ToggleRow(
                label = "WebView back first",
                checked = config.webViewBackFirst,
                onCheckedChange = { onConfigChanged(config.copy(webViewBackFirst = it)) },
            )
            ToggleRow(
                label = "Start fullscreen",
                checked = config.startFullscreen,
                onCheckedChange = { onConfigChanged(config.copy(startFullscreen = it)) },
            )
        }

        item {
            HorizontalDivider()
            PanelHeader("User agent")
            EnumChipRow(
                label = "User agent mode",
                value = config.userAgentMode,
                values = UserAgentMode.entries,
                onValueChanged = { onConfigChanged(config.copy(userAgentMode = it)) },
            )
            OutlinedTextField(
                value = config.customUserAgent,
                onValueChange = { onConfigChanged(config.copy(customUserAgent = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                label = { Text("Custom UA") },
                singleLine = false,
                enabled = config.userAgentMode == UserAgentMode.CUSTOM,
            )
        }

        item {
            HorizontalDivider()
            PanelHeader("Network and rendering")
            EnumChipRow(
                label = "Cache mode",
                value = config.cacheMode,
                values = WebCacheMode.entries,
                onValueChanged = { onConfigChanged(config.copy(cacheMode = it)) },
            )
            EnumChipRow(
                label = "Mixed content",
                value = config.mixedContentMode,
                values = MixedContentMode.entries,
                onValueChanged = { onConfigChanged(config.copy(mixedContentMode = it)) },
            )
            EnumChipRow(
                label = "Color mode",
                value = config.colorMode,
                values = WebColorMode.entries,
                onValueChanged = { onConfigChanged(config.copy(colorMode = it)) },
            )
        }

        item {
            HorizontalDivider()
            PanelHeader("Permissions")
            EnumChipRow(
                label = "Geolocation",
                value = config.geolocationPolicy,
                values = WebPermissionPolicy.entries,
                onValueChanged = { onConfigChanged(config.copy(geolocationPolicy = it)) },
            )
            EnumChipRow(
                label = "File chooser",
                value = config.fileChooserPolicy,
                values = FeaturePolicy.entries,
                onValueChanged = { onConfigChanged(config.copy(fileChooserPolicy = it)) },
            )
            EnumChipRow(
                label = "Camera",
                value = config.cameraPolicy,
                values = WebPermissionPolicy.entries,
                onValueChanged = { onConfigChanged(config.copy(cameraPolicy = it)) },
            )
            EnumChipRow(
                label = "Microphone",
                value = config.microphonePolicy,
                values = WebPermissionPolicy.entries,
                onValueChanged = { onConfigChanged(config.copy(microphonePolicy = it)) },
            )
        }
    }
}

@Composable
private fun PanelHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { option ->
                FilterChip(
                    selected = option == value,
                    onClick = { onValueChanged(option) },
                    label = { Text(option.displayName()) },
                )
            }
        }
    }
}

private fun Enum<*>.displayName(): String =
    name.lowercase(Locale.US)
        .split("_")
        .joinToString(" ") { word ->
            word.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.US) else first.toString()
            }
        }
