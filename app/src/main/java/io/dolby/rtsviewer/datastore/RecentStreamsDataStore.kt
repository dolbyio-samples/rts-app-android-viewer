package io.dolby.rtsviewer.datastore

import io.dolby.rtscomponentkit.domain.StreamingData
import kotlinx.coroutines.flow.Flow

interface RecentStreamsDataStore {
    var recentStreams: Flow<List<StreamDetail>>

    suspend fun recentStream(streamName: String) : StreamDetail?

    suspend fun addStreamDetail(streamDetail: StreamingData)

    suspend fun clearAll()
}
