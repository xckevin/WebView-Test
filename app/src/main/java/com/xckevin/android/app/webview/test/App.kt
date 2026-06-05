package com.xckevin.android.app.webview.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xckevin.android.app.webview.test.ui.navigation.ScannerRoute
import com.xckevin.android.app.webview.test.ui.navigation.SettingsRoute
import com.xckevin.android.app.webview.test.ui.navigation.WorkbenchRoute
import com.xckevin.android.app.webview.test.ui.scanner.ScannerScreen
import com.xckevin.android.app.webview.test.ui.settings.SettingsScreen
import com.xckevin.android.app.webview.test.ui.workbench.WorkbenchScreen

private const val SCAN_RESULT_KEY = "scanResult"

@Composable
fun WebViewTestApp(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WorkbenchRoute) {
        composable<WorkbenchRoute> { backStackEntry ->
            val scanResult by backStackEntry.savedStateHandle
                .getStateFlow<String?>(SCAN_RESULT_KEY, null)
                .collectAsStateWithLifecycle()
            WorkbenchScreen(
                container = container,
                scanResult = scanResult,
                onScanResultConsumed = {
                    backStackEntry.savedStateHandle[SCAN_RESULT_KEY] = null
                },
                onOpenScanner = { navController.navigate(ScannerRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ScannerRoute> {
            ScannerScreen(
                onResult = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SCAN_RESULT_KEY, it)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
