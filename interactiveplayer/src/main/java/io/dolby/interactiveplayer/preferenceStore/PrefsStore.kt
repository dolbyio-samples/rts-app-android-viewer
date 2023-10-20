package io.dolby.interactiveplayer.preferenceStore

import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.flow.Flow

interface PrefsStore {
    fun showSourceLabels(streamingData: StreamingData? = null): Flow<Boolean>
    fun multiviewLayout(streamingData: StreamingData? = null): Flow<MultiviewLayout>
    fun streamSourceOrder(streamingData: StreamingData? = null): Flow<StreamSortOrder>
    fun audioSelection(streamingData: StreamingData? = null): Flow<AudioSelection>
    fun showDebugOptions(): Flow<Boolean>

    suspend fun updateShowSourceLabels(show: Boolean, streamingData: StreamingData? = null)
    suspend fun updateMultiviewLayout(layout: MultiviewLayout, streamingData: StreamingData? = null)
    suspend fun updateStreamSourceOrder(
        order: StreamSortOrder,
        streamingData: StreamingData? = null
    )

    suspend fun updateAudioSelection(
        selection: AudioSelection,
        streamingData: StreamingData? = null
    )

    suspend fun updateShowDebugOptions(show: Boolean)

    suspend fun clear(streamingData: StreamingData)
    suspend fun clearAllStreamSettings()
}
