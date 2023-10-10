package io.dolby.interactiveplayer.preferenceStore

import io.dolby.interactiveplayer.R
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

    fun stringResource(): Int {
        return when(this) {
            ListView -> R.string.settings_multiview_layout_list_view
            SingleStreamView -> R.string.settings_multiview_layout_single_view
            GridView -> R.string.settings_multiview_layout_grid_view
        }
    }
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

    fun stringResource(): Int {
        return when(this) {
            ConnectionOrder -> R.string.settings_stream_sort_order_connection
            AlphaNumeric -> R.string.settings_stream_sort_order_alphanumeric
        }
    }

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

    fun stringResource(): Int {
        return when(this) {
            FirstSource -> R.string.settings_audio_selection_first_source
            FollowVideo -> R.string.settings_audio_selection_follow_video
            MainSource -> R.string.settings_audio_selection_main_source
        }
    }

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
