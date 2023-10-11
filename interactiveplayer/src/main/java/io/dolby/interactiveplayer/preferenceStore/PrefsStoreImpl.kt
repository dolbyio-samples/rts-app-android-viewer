package io.dolby.interactiveplayer.preferenceStore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private const val USER_PREFERENCES_STORE_NAME = "user_preferences"

class PrefsStoreImpl constructor(private val context: Context) : PrefsStore {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val preferences = mutableMapOf<String, PrefsImpl>()

    private fun key(streamingData: StreamingData?): String =
        streamingData?.let { "${streamingData.streamName}/${streamingData.accountId}" }
            ?: USER_PREFERENCES_STORE_NAME

    private fun streamPreferences(key: String): PrefsImpl {
        return preferences.getOrElse(key) {
            val createdStreamPreference = createPreferences(key)
            preferences[key] = createdStreamPreference
            createdStreamPreference
        }
    }

    private fun createPreferences(key: String): PrefsImpl {
        val result = PrefsImpl(context, key)
        preferences[USER_PREFERENCES_STORE_NAME]?.let {
            coroutineScope.launch {
                result.registerDefaultPreferenceValuesIfNeeded(
                    it.isLiveIndicatorEnabled.last(),
                    it.showSourceLabels.last(),
                    it.multiviewLayout.last().name,
                    it.streamSourceOrder.last().name,
                    it.audioSelection.last().name
                )
            }
        } ?: coroutineScope.launch {
            result.registerDefaultPreferenceValuesIfNeeded(
                isLiveIndicatorEnabledDefault = true,
                showSourceLabelsDefault = true,
                multiviewLayoutDefault = MultiviewLayout.default.name,
                sortOrderDefault = StreamSortOrder.default.name,
                audioSelectionDefault = AudioSelection.default.name
            )
        }
        return result
    }

    override fun isLiveIndicatorEnabled(streamingData: StreamingData?): Flow<Boolean> =
        streamPreferences(key(streamingData)).isLiveIndicatorEnabled

    override fun showSourceLabels(streamingData: StreamingData?): Flow<Boolean> =
        streamPreferences(key(streamingData)).showSourceLabels

    override fun multiviewLayout(streamingData: StreamingData?): Flow<MultiviewLayout> =
        streamPreferences(key(streamingData)).multiviewLayout

    override fun streamSourceOrder(streamingData: StreamingData?): Flow<StreamSortOrder> =
        streamPreferences(key(streamingData)).streamSourceOrder

    override fun audioSelection(streamingData: StreamingData?): Flow<AudioSelection> =
        streamPreferences(key(streamingData)).audioSelection

    override suspend fun updateLiveIndicator(checked: Boolean, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateLiveIndicator(checked)
    }

    override suspend fun updateShowSourceLabels(show: Boolean, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateShowSourceLabels(show)
    }

    override suspend fun updateMultiviewLayout(layout: MultiviewLayout, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateMultiviewLayout(layout)
    }

    override suspend fun updateStreamSourceOrder(order: StreamSortOrder, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateStreamSourceOrder(order)
    }

    override suspend fun updateAudioSelection(selection: AudioSelection, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateAudioSelection(selection)
    }

    override suspend fun clear(streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).clear()
        preferences.remove(key(streamingData))
    }
}

class PrefsImpl constructor(private val context: Context, prefsKey: String) : Prefs {

    private val Context.dataStore by preferencesDataStore(prefsKey)

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

    override suspend fun clear() {
        context.dataStore.edit { clear() }
    }

    suspend fun registerDefaultPreferenceValuesIfNeeded(
        isLiveIndicatorEnabledDefault: Boolean,
        showSourceLabelsDefault: Boolean,
        multiviewLayoutDefault: String,
        sortOrderDefault: String,
        audioSelectionDefault: String
    ) {
        context.dataStore.edit { userPreferences ->
            if (userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] == null) {
                userPreferences[PreferencesKeys.SHOW_LIVE_INDICATOR] =
                    isLiveIndicatorEnabledDefault
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

private object PreferencesKeys {
    val SHOW_LIVE_INDICATOR = booleanPreferencesKey("show_live_indicator")
    val SHOW_SOURCE_LABELS = booleanPreferencesKey("show_source_labels")
    val MULTIVIEW_LAYOUT = stringPreferencesKey("multiview_layout")
    val SORT_ORDER = stringPreferencesKey("sort_order")
    val AUDIO_SELECTION = stringPreferencesKey("audio_selection")
}
