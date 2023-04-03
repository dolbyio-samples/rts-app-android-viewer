package io.dolby.rtscomponentkit.data

import android.content.Context
import com.millicast.Media
import com.millicast.Subscriber
import io.dolby.rtscomponentkit.manager.SubscriptionManagerInterface

interface MillicastSdk {
    fun init(context: Context)

    fun getMedia(context: Context): Media
    fun initSubscriptionManager(subscriptionDelegate: Subscriber.Listener): SubscriptionManagerInterface
}
