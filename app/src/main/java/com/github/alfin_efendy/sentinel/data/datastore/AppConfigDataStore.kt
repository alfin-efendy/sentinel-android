package com.github.alfin_efendy.sentinel.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sentinel_config")

class AppConfigDataStore(private val context: Context) {

    private object Keys {
        val PACKAGE_NAME = stringPreferencesKey("package_name")
        val DEEP_LINK_URL = stringPreferencesKey("deep_link_url")
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val DEBOUNCE_MS = longPreferencesKey("debounce_ms")
        val GRACE_PERIOD_MS = longPreferencesKey("grace_period_ms")
        val MAX_RELAUNCHES = intPreferencesKey("max_relaunches")
        val WINDOW_DURATION_MS = longPreferencesKey("window_duration_ms")
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            packageName = prefs[Keys.PACKAGE_NAME] ?: "",
            deepLinkUrl = prefs[Keys.DEEP_LINK_URL]?.takeIf { it.isNotBlank() },
            isEnabled = prefs[Keys.IS_ENABLED] ?: false,
            debounceMs = prefs[Keys.DEBOUNCE_MS] ?: 2500L,
            gracePeriodMs = prefs[Keys.GRACE_PERIOD_MS] ?: 8000L,
            maxRelaunchesPerWindow = prefs[Keys.MAX_RELAUNCHES] ?: 3,
            windowDurationMs = prefs[Keys.WINDOW_DURATION_MS] ?: 60_000L
        )
    }

    suspend fun save(config: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PACKAGE_NAME] = config.packageName
            prefs[Keys.DEEP_LINK_URL] = config.deepLinkUrl ?: ""
            prefs[Keys.IS_ENABLED] = config.isEnabled
            prefs[Keys.DEBOUNCE_MS] = config.debounceMs
            prefs[Keys.GRACE_PERIOD_MS] = config.gracePeriodMs
            prefs[Keys.MAX_RELAUNCHES] = config.maxRelaunchesPerWindow
            prefs[Keys.WINDOW_DURATION_MS] = config.windowDurationMs
        }
    }

    /** Reads the current config once (suspending). Used by BootReceiver via goAsync(). */
    suspend fun readOnce(): AppConfig = configFlow.first()
}
