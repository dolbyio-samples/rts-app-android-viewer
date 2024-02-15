package io.dolby.interactiveplayer.datastore

import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.StreamingData
import kotlinx.coroutines.flow.Flow

interface RecentStreamsDataStore {
    var recentStreams: Flow<List<StreamDetail>>

    suspend fun recentStream(accountId: String, streamName: String): StreamDetail?

    fun addStreamDetail(streamingData: StreamingData, connectOptions: ConnectOptions)

    suspend fun clearAll()

    suspend fun clear(streamDetail: StreamDetail)
}
