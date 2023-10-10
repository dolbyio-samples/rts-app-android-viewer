package io.dolby.interactiveplayer.preferenceStore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private const val USER_PREFERENCES_STORE_NAME = "user_preferences"

class PrefsStoreImpl constructor(
    private val context: Context,
    basePrefs: PrefsStore? = null,
    preferencesName: String = USER_PREFERENCES_STORE_NAME
) : PrefsStore {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val Context.dataStore by preferencesDataStore(name = preferencesName)

    private object PreferencesKeys {
        val SHOW_LIVE_INDICATOR = booleanPreferencesKey("show_live_indicator")
        val SHOW_SOURCE_LABELS = booleanPreferencesKey("show_source_labels")
        val MULTIVIEW_LAYOUT = stringPreferencesKey("multiview_layout")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val AUDIO_SELECTION = stringPreferencesKey("audio_selection")
    }

    init {
        var isLiveIndicatorEnabledDefault = true
        var showSourceLabelsDefault = true
        var multiviewLayoutDefault = MultiviewLayout.default.name
        var sortOrderDefault = StreamSortOrder.default.name
        var audioSelectionDefault = AudioSelection.default.name
        basePrefs?.let {
            coroutineScope.launch {
                isLiveIndicatorEnabledDefault = it.isLiveIndicatorEnabled.last()
                showSourceLabelsDefault = it.showSourceLabels.last()
                multiviewLayoutDefault = it.multiviewLayout.last().name
                sortOrderDefault = it.streamSourceOrder.last().name
                audioSelectionDefault = it.audioSelection.last().name
            }
        }
        // Register default preference values, if it does not exist
        coroutineScope.launch {
            context.dataStore.edit { userPreferences ->
                if (userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] == null) {
                    userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] = isLiveIndicatorEnabledDefault
                }
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

    override suspend fun updateLiveIndicator(checked: Boolean) {
        context.dataStore.edit { userPreferences ->
            userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] = checked
        }
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
}
