package io.dolby.interactiveplayer.preferenceStore

import kotlinx.coroutines.flow.Flow
import kotlin.Boolean
import kotlin.Exception
import kotlin.String

interface PrefsStore {
    val isLiveIndicatorEnabled: Flow<Boolean>
    val showSourceLabels: Flow<Boolean>
    val multiviewLayout: Flow<MultiviewLayout>
    val streamSourceOrder: Flow<StreamSortOrder>
    val audioSelection: Flow<AudioSelection>

    suspend fun updateLiveIndicator(checked: Boolean)
    suspend fun updateShowSourceLabels(show: Boolean)
    suspend fun updateMultiviewLayout(layout: MultiviewLayout)
    suspend fun updateStreamSourceOrder(order: StreamSortOrder)
    suspend fun updateAudioSelection(selection: AudioSelection)
}

enum class MultiviewLayout {
    ListView, SingleStreamView, GridView;

    companion object {
        fun valueToLayout(value: String): MultiviewLayout {
            return try {
                MultiviewLayout.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }
        val default: MultiviewLayout = ListView
    }
}

enum class StreamSortOrder {
    ConnectionOrder, AlphaNumeric;

    companion object {
        fun valueToSortOrder(value: String): StreamSortOrder {
            return try {
                StreamSortOrder.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }

        val default: StreamSortOrder = ConnectionOrder
    }
}

enum class AudioSelection {
    FirstSource, FollowVideo, MainSource;

    companion object {
        fun valueToSelection(value: String): AudioSelection {
            return try {
                AudioSelection.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }
        val default: AudioSelection = MainSource
    }
}
