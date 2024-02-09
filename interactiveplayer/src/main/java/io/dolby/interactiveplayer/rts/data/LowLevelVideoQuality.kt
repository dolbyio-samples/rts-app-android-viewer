package io.dolby.interactiveplayer.rts.data

import com.millicast.subscribers.state.LayerData

sealed class LowLevelVideoQuality(val layerData: LayerData?) {
    open fun videoQuality() = VideoQuality.AUTO
    class Auto : LowLevelVideoQuality(null)
    class Low(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
        override fun videoQuality() = VideoQuality.LOW
    }

    class Medium(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
        override fun videoQuality() = VideoQuality.MEDIUM
    }

    class High(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
        override fun videoQuality() = VideoQuality.HIGH
    }
}
