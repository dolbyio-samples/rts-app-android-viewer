package io.dolby.rtsviewer.preferenceStore

import kotlinx.coroutines.flow.Flow

interface PrefsStore {
    val isLiveIndicatorEnabled: Flow<Boolean>

    suspend fun updateLiveIndicator(checked: Boolean)
}
