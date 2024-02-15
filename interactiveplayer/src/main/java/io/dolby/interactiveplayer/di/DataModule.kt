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
import com.millicast.Core
import com.millicast.Media
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStoreImpl
import io.dolby.interactiveplayer.preferenceStore.PrefsStoreImpl
import io.dolby.interactiveplayer.utils.NetworkStatusObserver
import io.dolby.interactiveplayer.utils.NetworkStatusObserverImpl
import io.dolby.rtscomponentkit.data.MillicastSdk
import io.dolby.rtscomponentkit.data.multistream.MultiStreamingRepository
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiStreamPrefsStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideMillicastSdk(): MillicastSdk {
        val result = object : MillicastSdk {
            override fun getMedia(): Media = Media
        }

        Core.initialize()
        return result
    }

    @Provides
    @Singleton
    fun provideMultiStreamingDataStore(
        @ApplicationContext context: Context,
        millicastSdk: MillicastSdk,
        prefsStore: MultiStreamPrefsStore,
        dispatcherProvider: DispatcherProvider
    ): MultiStreamingRepository {
        millicastSdk.getMedia()
        return MultiStreamingRepository(context, prefsStore, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): MultiStreamPrefsStore {
        return PrefsStoreImpl(context)
    }

    @Provides
    fun provideRecentStreamsDataStore(
        @ApplicationContext context: Context,
        prefsStore: MultiStreamPrefsStore,
        dispatcherProvider: DispatcherProvider
    ): RecentStreamsDataStore {
        return RecentStreamsDataStoreImpl(context, prefsStore, dispatcherProvider)
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
