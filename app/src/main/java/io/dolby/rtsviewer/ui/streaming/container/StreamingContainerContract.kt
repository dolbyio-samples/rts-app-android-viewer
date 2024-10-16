package io.dolby.rtsviewer.ui.streaming.container

import io.dolby.rtscomponentkit.domain.StreamConfigList
import io.dolby.rtsviewer.ui.streaming.common.AvailableStreamQuality
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamStateInfo

// state
data class StreamingContainerState(
    val streamInfos: List<StreamStateInfo> = emptyList(),
    val streamError: StreamError.NoInternetConnection? = null,
    val showSettings: Boolean = false,
    val showSimulcastSettings: Boolean = false,
    val showStatistics: Boolean = false,
    val selectedStreamQuality: AvailableStreamQuality = AvailableStreamQuality.AUTO
)

// ui state
data class StreamingContainerUiState(
    val streams: StreamConfigList,
    val streamError: StreamError.NoInternetConnection?,
    val shouldStayOn: Boolean,
    val requestSettingsFocus: Boolean,
    val showSettings: Boolean,
    val showSimulcastSettings: Boolean,
    val showStatistics: Boolean,
    val statisticsEnabled: Boolean,
    val showStatisticsBtn: Boolean, /* TODO: this is a flag until work is complete */
    val selectedStreamQualityTitleId: Int,
    val availableStreamQualityItems: List<AvailableStreamQuality>,
    val simulcastSettingsEnabled: Boolean
)

// actions

sealed class StreamingContainerAction {
    data class UpdateSimulcastSettingsVisibility(val show: Boolean) : StreamingContainerAction()
    object HideSettings : StreamingContainerAction()
    data class UpdateStatisticsVisibility(val show: Boolean) : StreamingContainerAction()
    data class UpdateSelectedStreamQuality(val streamQualityType: AvailableStreamQuality) :
        StreamingContainerAction()
}
