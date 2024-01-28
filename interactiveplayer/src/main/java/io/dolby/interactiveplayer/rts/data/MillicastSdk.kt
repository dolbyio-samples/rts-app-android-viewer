package io.dolby.interactiveplayer.rts.data

import com.millicast.Media
import com.millicast.subscribers.state.LayerData
import io.dolby.interactiveplayer.rts.domain.StreamingData

interface MillicastSdk {
    fun getMedia(): Media
}

interface SubscriptionManagerInterface {
    suspend fun connect(streamingData: StreamingData): Boolean
    suspend fun startSubscribe(): Boolean
    suspend fun stopSubscribe(): Boolean
    suspend fun selectLayer(layer: LayerData?): Boolean
}
