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
package io.dolby.interactiveplayer.di

import android.content.Context
import com.millicast.Client
import com.millicast.Media
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStoreImpl
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.preferenceStore.PrefsStoreImpl
import io.dolby.interactiveplayer.rts.data.MillicastSdk
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.utils.DispatcherProvider
import io.dolby.interactiveplayer.rts.utils.DispatcherProviderImpl
import io.dolby.interactiveplayer.utils.NetworkStatusObserver
import io.dolby.interactiveplayer.utils.NetworkStatusObserverImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideMillicastSdk(@ApplicationContext context: Context): MillicastSdk {
        val result = object : MillicastSdk {
            override fun getMedia(context: Context): Media = Media.getInstance(context)
        }
        Client.initMillicastSdk(context)
        return result
    }

    @Provides
    @Singleton
    fun provideMultiStreamingDataStore(millicastSdk: MillicastSdk): MultiStreamingRepository =
        MultiStreamingRepository()

    @Provides
    @Singleton
    fun provideGlobalPreferencesDataStore(@ApplicationContext context: Context): PrefsStore {
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
