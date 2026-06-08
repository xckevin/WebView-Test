package com.xckevin.android.app.webview.test

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.xckevin.android.app.webview.test.data.AppSettingsStore
import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.data.MIGRATION_1_2_DROP_TEST_CASES
import com.xckevin.android.app.webview.test.data.RoomHistoryRepository
import com.xckevin.android.app.webview.test.data.WebViewTestDatabase

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: WebViewTestDatabase = Room.databaseBuilder(
        applicationContext,
        WebViewTestDatabase::class.java,
        "webview_test.db",
    )
        .addMigrations(MIGRATION_1_2_DROP_TEST_CASES)
        .build()

    val historyRepository: HistoryRepository =
        RoomHistoryRepository(database.historyDao())

    val settingsStore: AppSettingsStore =
        AppSettingsStore(applicationContext)
}

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        AppContainer(context.applicationContext)
    }
}
