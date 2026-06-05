package com.xckevin.android.app.webview.test.ui.settings

import com.xckevin.android.app.webview.test.FakeHistoryRepository
import com.xckevin.android.app.webview.test.data.AppSettings
import com.xckevin.android.app.webview.test.data.SettingsRepository
import com.xckevin.android.app.webview.test.model.WebTestConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun debuggingSwitchDefaultsToFalse() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.state.value.webContentsDebuggingEnabled)
    }

    @Test fun enablingDebuggingUpdatesSettingsStore() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = viewModel(settingsRepository = settingsRepository)

        viewModel.setWebContentsDebuggingEnabled(true)
        advanceUntilIdle()

        assertTrue(settingsRepository.current.webContentsDebuggingEnabled)
        assertTrue(viewModel.state.value.webContentsDebuggingEnabled)
    }

    @Test fun resetHistoryCallsHistoryRepository() = runTest {
        val historyRepository = FakeHistoryRepository()
        val viewModel = viewModel(historyRepository = historyRepository)

        viewModel.resetHistory()
        advanceUntilIdle()

        assertEquals(1, historyRepository.clearCount)
    }

    @Test fun resetWebDefaultsStoresDefaultConfig() = runTest {
        val settingsRepository = FakeSettingsRepository(
            AppSettings.default().copy(
                defaultConfig = WebTestConfig.default().copy(javaScriptEnabled = false),
            )
        )
        val viewModel = viewModel(settingsRepository = settingsRepository)

        viewModel.resetWebDefaults()
        advanceUntilIdle()

        assertEquals(WebTestConfig.default(), settingsRepository.current.defaultConfig)
    }

    private fun viewModel(
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        historyRepository: FakeHistoryRepository = FakeHistoryRepository(),
    ) = SettingsViewModel(
        settingsRepository = settingsRepository,
        historyRepository = historyRepository,
    )
}

private class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings.default(),
) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)
    val current: AppSettings get() = mutableSettings.value

    override val settings: Flow<AppSettings> = mutableSettings

    override suspend fun setWebContentsDebuggingEnabled(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(webContentsDebuggingEnabled = enabled)
    }

    override suspend fun setAutoLoadScannedUrl(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(autoLoadScannedUrl = enabled)
    }

    override suspend fun setStartInFullscreen(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(startInFullscreen = enabled)
    }

    override suspend fun setDefaultConfig(config: WebTestConfig) {
        mutableSettings.value = mutableSettings.value.copy(defaultConfig = config)
    }
}
