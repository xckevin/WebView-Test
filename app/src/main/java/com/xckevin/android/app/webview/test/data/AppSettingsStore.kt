package com.xckevin.android.app.webview.test.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xckevin.android.app.webview.test.model.WebTestConfig
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class AppSettings(
    val webContentsDebuggingEnabled: Boolean,
    val autoLoadScannedUrl: Boolean,
    val startInFullscreen: Boolean,
    val defaultConfig: WebTestConfig,
)

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val defaultConfig = preferences[Keys.DEFAULT_CONFIG]?.let { decodeDefaultConfig(it) }
                ?: WebTestConfig.default()

            AppSettings(
                webContentsDebuggingEnabled = preferences[Keys.WEB_CONTENTS_DEBUGGING_ENABLED] ?: false,
                autoLoadScannedUrl = preferences[Keys.AUTO_LOAD_SCANNED_URL] ?: true,
                startInFullscreen = preferences[Keys.START_IN_FULLSCREEN] ?: false,
                defaultConfig = defaultConfig,
            )
        }

    suspend fun setWebContentsDebuggingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.WEB_CONTENTS_DEBUGGING_ENABLED] = enabled
        }
    }

    suspend fun setAutoLoadScannedUrl(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_LOAD_SCANNED_URL] = enabled
        }
    }

    suspend fun setStartInFullscreen(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.START_IN_FULLSCREEN] = enabled
        }
    }

    suspend fun setDefaultConfig(config: WebTestConfig) {
        dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_CONFIG] = json.encodeToString(config)
        }
    }

    private fun decodeDefaultConfig(raw: String): WebTestConfig =
        try {
            json.decodeFromString<WebTestConfig>(raw)
        } catch (_: SerializationException) {
            WebTestConfig.default()
        }

    private object Keys {
        val WEB_CONTENTS_DEBUGGING_ENABLED = booleanPreferencesKey("web_contents_debugging_enabled")
        val AUTO_LOAD_SCANNED_URL = booleanPreferencesKey("auto_load_scanned_url")
        val START_IN_FULLSCREEN = booleanPreferencesKey("start_in_fullscreen")
        val DEFAULT_CONFIG = stringPreferencesKey("default_config")
    }

    companion object {
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    }
}
