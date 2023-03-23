package io.dolby.rtsviewer.datastore

import kotlinx.coroutines.flow.Flow

interface RecentStreamsDataStore {
    var recentStreams: Flow<List<StreamDetail>>

    suspend fun addStreamDetail(streamName: String, accountID: String)

    suspend fun clearAll()
}
