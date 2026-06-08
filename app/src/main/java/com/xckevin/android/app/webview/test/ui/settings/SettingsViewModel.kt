package com.xckevin.android.app.webview.test.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.data.SettingsRepository
import com.xckevin.android.app.webview.test.model.WebTestConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val webContentsDebuggingEnabled: Boolean = false,
    val defaultConfig: WebTestConfig = WebTestConfig.default(),
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        webContentsDebuggingEnabled = settings.webContentsDebuggingEnabled,
                        defaultConfig = settings.defaultConfig,
                    )
                }
            }
        }
    }

    fun setWebContentsDebuggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWebContentsDebuggingEnabled(enabled)
            _state.update {
                it.copy(statusMessage = if (enabled) "WebView debugging enabled" else "WebView debugging disabled")
            }
        }
    }

    fun resetHistory() {
        viewModelScope.launch {
            historyRepository.clear()
            _state.update { it.copy(statusMessage = "History cleared") }
        }
    }

    fun resetWebDefaults() {
        viewModelScope.launch {
            settingsRepository.setDefaultConfig(WebTestConfig.default())
            _state.update { it.copy(statusMessage = "Default WebView config restored") }
        }
    }

    fun updateDefaultConfig(config: WebTestConfig) {
        viewModelScope.launch {
            settingsRepository.setDefaultConfig(config)
        }
    }

    fun clearDebugLogs() {
        _state.update {
            it.copy(statusMessage = "Debug logs are session scoped. Clear the active session from the Debug panel.")
        }
    }

    fun clearCookies() {
        _state.update { it.copy(statusMessage = "Clearing cookies") }
    }

    fun clearWebViewCache() {
        _state.update { it.copy(statusMessage = "Clearing WebView cache") }
    }

    fun setStatusMessage(message: String) {
        _state.update { it.copy(statusMessage = message) }
    }
}
