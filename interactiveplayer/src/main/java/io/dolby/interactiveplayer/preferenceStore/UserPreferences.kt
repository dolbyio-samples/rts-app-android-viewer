package io.dolby.interactiveplayer.preferenceStore

data class UserPreferences(
    val showLiveIndicator: Boolean,
    val showSourceLabels: Boolean,
    val multiviewLayout: MultiviewLayout,
    val sortOrder: StreamSortOrder,
    val audioSelection: AudioSelection
)
