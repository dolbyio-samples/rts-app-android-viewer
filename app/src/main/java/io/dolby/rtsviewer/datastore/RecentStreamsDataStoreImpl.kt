package io.dolby.rtsviewer.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.dolby.rtscomponentkit.domain.StreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_SAVED_STREAMS_LIMIT = 25

class RecentStreamsDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RecentStreamsDataStore {
    private val dataStore = context.recentStreamsDataStore
    private val appCoroutineScope = CoroutineScope(Dispatchers.IO)

    override var recentStreams: Flow<List<StreamDetail>> =
        dataStore.data.map {
            it.streamDetailList.sortedByDescending { streamDetail -> streamDetail.lastUsedDate.seconds }
        }

    override suspend fun recentStream(streamName: String): StreamDetail? {
        return dataStore.data.first().streamDetailList.firstOrNull {
            it.streamName == streamName
        }
    }

    override suspend fun addStreamDetail(streamingData: StreamingData) {
        val addStreamJob = appCoroutineScope.launch {
            dataStore.updateData {
                // Remove existing stream matching the new stream details
                val matchingIndex = it.streamDetailList
                    .indexOfFirst { streamDetail ->
                        streamDetail.streamName == streamingData.streamName && streamDetail.accountID == streamingData.accountId
                    }

                var builder = it.toBuilder()
                if (matchingIndex != -1) {
                    builder = builder.removeStreamDetail(matchingIndex)
                }

                // Add the stream detail
                val unixTime = System.currentTimeMillis()
                val timeStamp = com.google.protobuf.Timestamp
                    .newBuilder()
                    .setSeconds(unixTime)
                    .build()
                val newStreamDetail = StreamDetail.newBuilder()
                    .setStreamName(streamingData.streamName)
                    .setAccountID(streamingData.accountId)
                    .setLastUsedDate(timeStamp)
                    .build()
                builder = builder.addStreamDetail(0, newStreamDetail)

                // Commit the changes
                builder.build()
            }
        }

        appCoroutineScope.launch {
            addStreamJob.join()

            dataStore.updateData {
                var builder = it.toBuilder()

                // Remove streams from index - 25 onwards to keep the saved streams to a max limit of 25
                val numberOfSavedStreams = it.streamDetailList.count()
                val maxPermissibleIndex = MAX_SAVED_STREAMS_LIMIT - 1
                var lastIndex = numberOfSavedStreams - 1

                while (lastIndex > maxPermissibleIndex) {
                    builder = builder.removeStreamDetail(lastIndex)
                    lastIndex -= 1
                }

                // Commit the changes
                builder.build()
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
