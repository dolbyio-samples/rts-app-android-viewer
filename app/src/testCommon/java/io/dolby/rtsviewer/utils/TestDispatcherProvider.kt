package io.dolby.rtsviewer.utils

import io.dolby.rtscomponentkit.utils.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider constructor(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : DispatcherProvider {

    override val default
        get() = testDispatcher
    override val io
        get() = testDispatcher
    override val main
        get() = testDispatcher
}
