package io.dolby.interactiveplayer.preferenceStore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private const val USER_PREFERENCES_STORE_NAME = "user_preferences"

class PrefsStoreImpl constructor(private val context: Context) : PrefsStore {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val Context.dataStore by preferencesDataStore(name = USER_PREFERENCES_STORE_NAME)

    private object PreferencesKeys {
        val SHOW_LIVE_INDICATOR = booleanPreferencesKey("show_live_indicator")
    }

    init {
        // Register default preference values, if it does not exist
        coroutineScope.launch {
            context.dataStore.edit { userPreferences ->
                if (userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] == null) {
                    userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] = true
                }
            }
        }
    }

    override val isLiveIndicatorEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            // Defaults to `true` if the setting does not exist yet - thus shows the live indicator by default on first use
            val showLiveIndicator = preferences[PreferencesKeys.SHOW_LIVE_INDICATOR] ?: true
            showLiveIndicator
        }

    override suspend fun updateLiveIndicator(checked: Boolean) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] = checked
        }
    }
}
