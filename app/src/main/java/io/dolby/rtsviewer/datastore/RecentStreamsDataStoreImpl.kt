package io.dolby.rtsviewer.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    override suspend fun addStreamDetail(streamName: String, accountID: String) {
        val removeMatchingStreamJob = appCoroutineScope.launch {
            dataStore.updateData {
                val matchingIndex = it.streamDetailList
                    .indexOfFirst { streamDetail ->
                        streamDetail.streamName == streamName && streamDetail.accountID == accountID
                    }

                var builder = it.toBuilder()
                if (matchingIndex != -1) {
                    builder = builder.removeStreamDetail(matchingIndex)
                }

                builder.build()
            }
        }

        // Remove streams from index - 25 onwards to keep the saved streams to a max limit of 25
        val removeStreamsBeyondMaxLimit = appCoroutineScope.launch {
            removeMatchingStreamJob.join()

            dataStore.updateData {
                val numberOfSavedStreams = it.streamDetailList.count()
                val maxPermissibleIndex = MAX_SAVED_STREAMS_LIMIT - 1

                var builder = it.toBuilder()
                var indexOfLastStream = numberOfSavedStreams - 1

                while (indexOfLastStream >= maxPermissibleIndex) {
                    builder = builder.removeStreamDetail(indexOfLastStream)
                    indexOfLastStream -= 1
                }

                builder.build()
            }
        }

        appCoroutineScope.launch {
            removeStreamsBeyondMaxLimit.join()

            dataStore.updateData {
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
