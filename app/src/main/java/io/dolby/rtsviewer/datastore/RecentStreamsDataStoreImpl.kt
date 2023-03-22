package io.dolby.rtsviewer.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class RecentStreamsDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RecentStreamsDataStore {
    private val dataStore = context.recentStreamsDataStore
    private val appCoroutineScope = CoroutineScope(Dispatchers.IO)

    override var recentStreams: Flow<List<StreamDetail>> =
        dataStore.data.map {
            it.streamDetailList.sortedBy { streamDetail -> streamDetail.lastUsedDate.seconds }
        }

    override suspend fun addStreamDetail(streamName: String, accountID: String) {
        appCoroutineScope.launch {
            dataStore.updateData {
                val matchingIndex = it.streamDetailList
                    .indexOfFirst { streamDetail ->
                        streamDetail.streamName == streamName && streamDetail.accountID == accountID
                    }

                if (matchingIndex != -1) {
                    it.toBuilder().removeStreamDetail(matchingIndex).build()
                }

                val unixTime = System.currentTimeMillis()
                val timeStamp = com.google.protobuf.Timestamp
                    .newBuilder()
                    .setSeconds(unixTime)
                    .build()
                val streamDetail = StreamDetail.newBuilder()
                    .setStreamName(streamName)
                    .setAccountID(accountID)
                    .setLastUsedDate(timeStamp)
                    .build()
                it.toBuilder().addStreamDetail(0, streamDetail).build()
            }
        }
    }

    override suspend fun clearAll() {
        appCoroutineScope.launch {
            dataStore.updateData {
                it.toBuilder().clear().build()
            }
        }
    }
}
