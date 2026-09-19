package org.centrexcursionistalcoi.app.di

import kotlinx.io.files.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Provides the platform-specific filesystem paths the app needs. Platform `single`s are bound in
 * each platform's `platformModule()` (in `ManualModules.<platform>.kt`).
 */
interface PathsProvider {
    /** The path where all app data should be stored at. */
    val systemDataPath: Path
}

private object PathsProviderHolder : KoinComponent {
    val provider: PathsProvider by inject()
}

/**
 * Accessor for [PathsProvider] from places that can't take constructor injection (top-level
 * objects/functions). Prefer constructor injection wherever the call site is a class Koin can build.
 */
val globalPathsProvider: PathsProvider get() = PathsProviderHolder.provider
