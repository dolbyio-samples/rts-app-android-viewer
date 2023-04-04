/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dolby.rtsviewer.di

import android.content.Context
import com.millicast.Client
import com.millicast.Media
import com.millicast.Subscriber
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.dolby.rtscomponentkit.data.MillicastSdk
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.manager.SubscriptionManager
import io.dolby.rtscomponentkit.manager.SubscriptionManagerInterface
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import io.dolby.rtsviewer.datastore.RecentStreamsDataStoreImpl
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.preferenceStore.PrefsStoreImpl
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import io.dolby.rtsviewer.utils.NetworkStatusObserverImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun provideMillicastSdk(): MillicastSdk = object : MillicastSdk {
        override fun init(context: Context) = Client.initMillicastSdk(context)

        override fun getMedia(context: Context): Media = Media.getInstance(context)

        override fun initSubscriptionManager(subscriptionDelegate: Subscriber.Listener): SubscriptionManagerInterface =
            SubscriptionManager(subscriptionDelegate)
    }

    @Provides
    @Singleton
    fun provideRTSRepository(
        @ApplicationContext context: Context,
        millicastSdk: MillicastSdk
    ): RTSViewerDataStore = RTSViewerDataStore(context, millicastSdk)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): PrefsStore {
        return PrefsStoreImpl(context)
    }

    @Provides
    fun provideRecentStreamsDataStore(@ApplicationContext context: Context): RecentStreamsDataStore {
        return RecentStreamsDataStoreImpl(context)
    }

    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return DispatcherProviderImpl
    }

    @Provides
    fun provideNetworkStatusObserver(@ApplicationContext context: Context): NetworkStatusObserver {
        return NetworkStatusObserverImpl(context)
    }
}
