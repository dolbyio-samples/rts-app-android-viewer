package io.dolby.interactiveplayer.rts.data

import com.millicast.subscribers.state.LayerData

sealed class LowLevelVideoQuality(
    val videoQuality: VideoQuality,
    val layerData: LayerData?
) {
    class Auto : LowLevelVideoQuality(VideoQuality.AUTO, null)

    class Low(layerData: LayerData?) : LowLevelVideoQuality(VideoQuality.LOW, layerData)

    class Medium(layerData: LayerData?) : LowLevelVideoQuality(VideoQuality.MEDIUM, layerData)

    class High(layerData: LayerData?) : LowLevelVideoQuality(VideoQuality.HIGH, layerData)
}