package io.dolby.interactiveplayer.preferenceStore

import androidx.datastore.preferences.core.Preferences
import io.dolby.rtscomponentkit.data.multistream.prefs.AudioSelection
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiviewLayout
import io.dolby.rtscomponentkit.data.multistream.prefs.StreamSortOrder
import kotlinx.coroutines.flow.Flow

interface Prefs {
    val data: Flow<Preferences>

    val showSourceLabels: Flow<Boolean>
    val multiviewLayout: Flow<MultiviewLayout>
    val streamSourceOrder: Flow<StreamSortOrder>
    val audioSelection: Flow<AudioSelection>
    val showDebugOptions: Flow<Boolean>

    suspend fun updateShowSourceLabels(show: Boolean)
    suspend fun updateMultiviewLayout(layout: MultiviewLayout)
    suspend fun updateStreamSourceOrder(order: StreamSortOrder)
    suspend fun updateAudioSelection(selection: AudioSelection)
    suspend fun updateShowDebugOptions(show: Boolean)

    suspend fun clearPreference()
}
