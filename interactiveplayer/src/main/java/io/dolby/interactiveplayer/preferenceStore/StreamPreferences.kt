package io.dolby.interactiveplayer.preferenceStore

import io.dolby.rtscomponentkit.data.multistream.prefs.AudioSelection
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiviewLayout
import io.dolby.rtscomponentkit.data.multistream.prefs.StreamSortOrder

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
