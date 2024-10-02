package io.dolby.interactiveplayer.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiStreamPrefsStore
import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_SAVED_STREAMS_LIMIT = 25

class RecentStreamsDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsStore: MultiStreamPrefsStore,
    dispatcherProvider: DispatcherProvider
) : RecentStreamsDataStore {
    private val dataStore = context.recentStreamsDataStore
    private val appCoroutineScope = CoroutineScope(dispatcherProvider.io)

    override var recentStreams: Flow<List<StreamDetail>> =
        dataStore.data.map {
            it.streamDetailList.sortedByDescending { streamDetail -> streamDetail.lastUsedDate.seconds }
        }

    override suspend fun recentStream(accountId: String, streamName: String): StreamDetail? {
        return dataStore.data.first().streamDetailList.firstOrNull {
            it.accountID == accountId && it.streamName == streamName
        }
    }

    override fun addStreamDetail(streamingData: StreamingData, connectOptions: ConnectOptions) {
        val addStreamJob = appCoroutineScope.launch {
            dataStore.updateData {
                // Remove existing stream matching the new stream details
                val matchingIndex = it.streamDetailList
                    .indexOfFirst { streamDetail ->
                        streamDetail.streamName == streamingData.streamName &&
                            streamDetail.accountID == streamingData.accountId
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
                    .setForcePlayOutDelay(connectOptions.forcePlayOutDelay)
                    .setDisableAudio(connectOptions.disableAudio)
                    .setRtcLogs(connectOptions.rtcLogs)
                    .setVideoJitterMinimumDelayMs(connectOptions.videoJitterMinimumDelayMs)
                    .setPrimaryVideoQuality(connectOptions.primaryVideoQuality.name)
                    .setServerEnv(connectOptions.mediaServerEnv.name)
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
        dataStore.updateData {
            it.toBuilder().clear().build()
        }
        prefsStore.clearAllStreamSettings()
    }

    override suspend fun clear(streamDetail: StreamDetail) {
        val index = dataStore.data.first().streamDetailList.indexOfFirst {
            streamDetail == it
        }
        if (index > -1) {
            dataStore.updateData {
                it.toBuilder().removeStreamDetail(index).build()
            }
            prefsStore.clear(StreamingData(streamDetail.accountID, streamDetail.streamName))
        }
    }
}
