package com.xckevin.android.app.webview.test.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xckevin.android.app.webview.test.AppContainer
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.ui.common.AppScaffold

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.screen_settings))
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.action_back))
            }
        }
    }
}
