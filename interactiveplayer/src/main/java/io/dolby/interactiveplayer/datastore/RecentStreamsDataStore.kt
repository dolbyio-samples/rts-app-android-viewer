package io.dolby.interactiveplayer.datastore

import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.flow.Flow

interface RecentStreamsDataStore {
    var recentStreams: Flow<List<StreamDetail>>

    suspend fun recentStream(streamName: String): StreamDetail?

    fun addStreamDetail(streamingData: StreamingData)

    suspend fun clearAll()

    suspend fun clear(streamDetail: StreamDetail)
}
