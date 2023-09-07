package io.dolby.interactiveplayer.rts.data

import android.content.Context
import com.millicast.LayerData
import com.millicast.Media
import io.dolby.interactiveplayer.rts.domain.StreamingData

interface MillicastSdk {
    fun getMedia(context: Context): Media
}
interface SubscriptionManagerInterface {
    suspend fun connect(streamingData: StreamingData): Boolean
    suspend fun startSubscribe(): Boolean
    suspend fun stopSubscribe(): Boolean
    suspend fun selectLayer(layer: LayerData?): Boolean
}
