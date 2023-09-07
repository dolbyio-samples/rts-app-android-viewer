package io.dolby.interactiveplayer.utils

import kotlinx.coroutines.flow.Flow

interface NetworkStatusObserver {
    enum class Status {
        Available, Unavailable
    }

    val status: Flow<Status>
}
