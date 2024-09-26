package io.dolby.rtsviewer.di

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.millicast.Media
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.dolby.rtscomponentkit.data.MillicastSdk
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import io.dolby.rtsviewer.datastore.RecentStreamsDataStoreImpl
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.utils.TestDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
class MockDataModule {
    @Provides
    fun provideMillicastSdk(): MillicastSdk = object : MillicastSdk {

        override fun getMedia(): Media {
            return mock()
        }
    }

    @Provides
    fun provideRTSRepository(
        @ApplicationContext context: Context,
        millicastSdk: MillicastSdk
    ): RTSViewerDataStore = RTSViewerDataStore(millicastSdk)

    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): PrefsStore =
        object : PrefsStore {
            val liveIndicatorState: MutableState<Boolean> = mutableStateOf(true)
            override var isLiveIndicatorEnabled: Flow<Boolean>
                get() = flow { emit(liveIndicatorState.value) }
                set(_) {}

            override suspend fun updateLiveIndicator(checked: Boolean) {
                liveIndicatorState.value = checked
            }
        }

    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return TestDispatcherProvider()
    }

    @Provides
    fun provideRecentStreamsDataStore(@ApplicationContext context: Context): RecentStreamsDataStore {
        return RecentStreamsDataStoreImpl(context)
    }
}
