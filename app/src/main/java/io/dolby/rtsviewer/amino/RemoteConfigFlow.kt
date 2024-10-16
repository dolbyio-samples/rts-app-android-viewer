package io.dolby.rtsviewer.amino

import io.dolby.rtscomponentkit.domain.StreamConfigList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Singleton

@Singleton
class RemoteConfigFlow {
    private val _config = MutableStateFlow(StreamConfigList(emptyList()))
    var config = _config.asStateFlow()

    fun updateConfig(config: StreamConfigList) {
        _config.update { config }
    }
}
