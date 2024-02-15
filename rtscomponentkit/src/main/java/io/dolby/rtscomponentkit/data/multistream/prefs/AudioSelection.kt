package io.dolby.rtscomponentkit.data.multistream.prefs

import io.dolby.rtscomponentkit.R

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
