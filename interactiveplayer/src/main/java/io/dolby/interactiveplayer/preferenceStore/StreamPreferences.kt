package io.dolby.interactiveplayer.preferenceStore

import io.dolby.interactiveplayer.R

data class GlobalPreferences(
    val showDebugOptions: Boolean,
    val showSourceLabels: Boolean,
    val multiviewLayout: MultiviewLayout,
    val sortOrder: StreamSortOrder,
    val audioSelection: AudioSelection
)

data class StreamPreferences(
    val showSourceLabels: Boolean,
    val multiviewLayout: MultiviewLayout,
    val sortOrder: StreamSortOrder,
    val audioSelection: AudioSelection
)

enum class MultiviewLayout {
    ListView, SingleStreamView, GridView;

    fun stringResource(): Int {
        return when (this) {
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
        return when (this) {
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

sealed class AudioSelection(val name: String) {
    object FirstSource : AudioSelection("FirstSource")
    object FollowVideo : AudioSelection("FollowVideo")
    object MainSource : AudioSelection("MainSource")

    class CustomAudioSelection(val sourceId: String) : AudioSelection(sourceId)

    fun stringResource(): Int? {
        return when (this) {
            FirstSource -> R.string.settings_audio_selection_first_source
            FollowVideo -> R.string.settings_audio_selection_follow_video
            MainSource -> R.string.settings_audio_selection_main_source
            is CustomAudioSelection -> null
        }
    }

    companion object {
        fun valueToSelection(value: String): AudioSelection {
            return when (value) {
                "FirstSource" -> FirstSource
                "FollowVideo" -> FollowVideo
                "MainSource" -> MainSource
                else -> CustomAudioSelection(sourceId = value)
            }
        }
        val default: AudioSelection = FirstSource
    }
}
