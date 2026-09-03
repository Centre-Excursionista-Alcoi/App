package org.centrexcursionistalcoi.app.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

@Single
class DefaultDispatcherProvider : DispatcherProvider {
    override val main = Dispatchers.Main
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
}

private object DispatcherProviderHolder : KoinComponent {
    val provider: DispatcherProvider by inject()
}

/**
 * Accessor for [DispatcherProvider] from places that can't take constructor injection (top-level
 * functions, platform `object`s implementing an external interface/callback). Prefer constructor
 * injection wherever the call site is a class Koin can build.
 */
val globalDispatcherProvider: DispatcherProvider get() = DispatcherProviderHolder.provider
