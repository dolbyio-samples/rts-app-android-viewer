package io.dolby.interactiveplayer.preferenceStore

import kotlinx.coroutines.flow.Flow

interface PrefsStore {
    val isLiveIndicatorEnabled: Flow<Boolean>

    suspend fun updateLiveIndicator(checked: Boolean)
}
