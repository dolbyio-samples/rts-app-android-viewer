package io.dolby.interactiveplayer.preferenceStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class PrefsImpl constructor(private val context: Context, prefsKey: String) : Prefs {

    private val Context.dataStore by preferencesDataStore(prefsKey)

    override val data = context.dataStore.data

    override val showSourceLabels: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val showSourceLabels = preferences[PreferencesKeys.SHOW_SOURCE_LABELS] ?: true
            showSourceLabels
        }

    override val multiviewLayout: Flow<MultiviewLayout> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.MULTIVIEW_LAYOUT]?.let { MultiviewLayout.valueToLayout(it) }
                ?: MultiviewLayout.default
        }

    override val streamSourceOrder: Flow<StreamSortOrder> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.SORT_ORDER]?.let { StreamSortOrder.valueToSortOrder(it) }
                ?: StreamSortOrder.default
        }

    override val audioSelection: Flow<AudioSelection> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.AUDIO_SELECTION]?.let { AudioSelection.valueToSelection(it) }
                ?: AudioSelection.default
        }

    override val showDebugOptions: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val showDebugOptions = preferences[PreferencesKeys.SHOW_DEBUG_OPTIONS] ?: false
            showDebugOptions
        }

    override suspend fun updateShowSourceLabels(show: Boolean) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.SHOW_SOURCE_LABELS] = show
        }
    }

    override suspend fun updateMultiviewLayout(layout: MultiviewLayout) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.MULTIVIEW_LAYOUT] = layout.name
        }
    }

    override suspend fun updateStreamSourceOrder(order: StreamSortOrder) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.SORT_ORDER] = order.name
        }
    }

    override suspend fun updateAudioSelection(selection: AudioSelection) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.AUDIO_SELECTION] = selection.name
        }
    }

    override suspend fun updateShowDebugOptions(show: Boolean) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.SHOW_DEBUG_OPTIONS] = show
        }
    }

    override suspend fun clearPreference() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun registerDefaultPreferenceValuesIfNeeded(
        showSourceLabelsDefault: Boolean,
        multiviewLayoutDefault: String,
        sortOrderDefault: String,
        audioSelectionDefault: String,
        showDebugOptions: Boolean?
    ) {
        context.dataStore.edit { userPreferences ->
            if (userPreferences[PreferencesKeys.SHOW_SOURCE_LABELS] == null) {
                userPreferences[PreferencesKeys.SHOW_SOURCE_LABELS] = showSourceLabelsDefault
            }
            if (userPreferences[PreferencesKeys.MULTIVIEW_LAYOUT] == null) {
                userPreferences[PreferencesKeys.MULTIVIEW_LAYOUT] = multiviewLayoutDefault
            }
            if (userPreferences[PreferencesKeys.SORT_ORDER] == null) {
                userPreferences[PreferencesKeys.SORT_ORDER] = sortOrderDefault
            }
            if (userPreferences[PreferencesKeys.AUDIO_SELECTION] == null) {
                userPreferences[PreferencesKeys.AUDIO_SELECTION] = audioSelectionDefault
            }
            showDebugOptions?.let {
                if (userPreferences[PreferencesKeys.SHOW_DEBUG_OPTIONS] == null) {
                    userPreferences[PreferencesKeys.SHOW_DEBUG_OPTIONS] = showDebugOptions
                }
            }
        }
    }
}
