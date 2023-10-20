package io.dolby.interactiveplayer.preferenceStore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val GLOBAL_PREFERENCES_NAME = "global_preferences"

class PrefsStoreImpl constructor(private val context: Context) : PrefsStore {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val preferences = mutableMapOf<String, PrefsImpl>()

    init {
        streamPreferences(key(null))
    }

    private fun key(streamingData: StreamingData?): String =
        streamingData?.let { "${streamingData.streamName}_${streamingData.accountId}" }
            ?: GLOBAL_PREFERENCES_NAME

    private fun streamPreferences(key: String): PrefsImpl {
        val prefs = preferences.getOrElse(key) {
            PrefsImpl(context, key)
        }
        prefillPreferences(prefs)
        preferences[key] = prefs
        return prefs
    }

    private fun prefillPreferences(prefs: PrefsImpl) {
        preferences[GLOBAL_PREFERENCES_NAME]?.let { globalSettings ->
            coroutineScope.launch {
                globalSettings.data.collectLatest {
                    prefs.registerDefaultPreferenceValuesIfNeeded(
                        it[PreferencesKeys.SHOW_SOURCE_LABELS] ?: true,
                        it[PreferencesKeys.MULTIVIEW_LAYOUT] ?: MultiviewLayout.default.name,
                        it[PreferencesKeys.SORT_ORDER] ?: StreamSortOrder.default.name,
                        it[PreferencesKeys.AUDIO_SELECTION] ?: AudioSelection.default.name,
                        null
                    )
                }
            }
        } ?: coroutineScope.launch {
            prefs.registerDefaultPreferenceValuesIfNeeded(
                showSourceLabelsDefault = true,
                multiviewLayoutDefault = MultiviewLayout.default.name,
                sortOrderDefault = StreamSortOrder.default.name,
                audioSelectionDefault = AudioSelection.default.name,
                showDebugOptions = false
            )
        }
    }

    override fun showDebugOptions(): Flow<Boolean> =
        streamPreferences(key(null)).showDebugOptions

    override fun showSourceLabels(streamingData: StreamingData?): Flow<Boolean> =
        streamPreferences(key(streamingData)).showSourceLabels

    override fun multiviewLayout(streamingData: StreamingData?): Flow<MultiviewLayout> =
        streamPreferences(key(streamingData)).multiviewLayout

    override fun streamSourceOrder(streamingData: StreamingData?): Flow<StreamSortOrder> =
        streamPreferences(key(streamingData)).streamSourceOrder

    override fun audioSelection(streamingData: StreamingData?): Flow<AudioSelection> =
        streamPreferences(key(streamingData)).audioSelection

    override suspend fun updateShowSourceLabels(show: Boolean, streamingData: StreamingData?) {
        streamPreferences(key(streamingData)).updateShowSourceLabels(show)
    }

    override suspend fun updateMultiviewLayout(
        layout: MultiviewLayout,
        streamingData: StreamingData?
    ) {
        streamPreferences(key(streamingData)).updateMultiviewLayout(layout)
    }

    override suspend fun updateStreamSourceOrder(
        order: StreamSortOrder,
        streamingData: StreamingData?
    ) {
        streamPreferences(key(streamingData)).updateStreamSourceOrder(order)
    }

    override suspend fun updateAudioSelection(
        selection: AudioSelection,
        streamingData: StreamingData?
    ) {
        streamPreferences(key(streamingData)).updateAudioSelection(selection)
    }

    override suspend fun updateShowDebugOptions(show: Boolean) {
        streamPreferences(key(null)).updateShowDebugOptions(show)
    }

    override suspend fun clear(streamingData: StreamingData) {
        streamPreferences(key(streamingData)).clearPreference()
    }

    override suspend fun clearAllStreamSettings() {
        preferences.forEach {
            if (it.key != GLOBAL_PREFERENCES_NAME) {
                streamPreferences(it.key).clearPreference()
            }
        }
    }
}

object PreferencesKeys {
    val SHOW_SOURCE_LABELS = booleanPreferencesKey("show_source_labels")
    val MULTIVIEW_LAYOUT = stringPreferencesKey("multiview_layout")
    val SORT_ORDER = stringPreferencesKey("sort_order")
    val AUDIO_SELECTION = stringPreferencesKey("audio_selection")

    val SHOW_DEBUG_OPTIONS = booleanPreferencesKey("show_debug_options")
    val USE_DEV_SERVER = booleanPreferencesKey("use_dev_server")
    val VIDEO_JITTER_BUFFER_MS = intPreferencesKey("video_jitter_buffer_ms")
    val FORCE_PLAYOUT_DELAY = booleanPreferencesKey("force_playout_delay")
    val DISABLE_AUDIO = booleanPreferencesKey("disable_audio")
    val PRIMARY_VIDEO_QUALITY = stringPreferencesKey("primary_video_quality")
    val SAVE_LOGS = booleanPreferencesKey("save_logs")
}
