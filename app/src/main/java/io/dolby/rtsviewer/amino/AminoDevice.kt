package io.dolby.rtsviewer.amino

import io.dolby.rtscomponentkit.domain.StreamingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Singleton

@Singleton
class AminoDevice {
    private val _config = MutableStateFlow<StreamingConfig?>(null)
    var config = _config.asStateFlow()

    fun updateConfig(config: StreamingConfig?) {
        _config.update { config }
    }
}
