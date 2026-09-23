package org.centrexcursionistalcoi.app.di

import kotlinx.io.files.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Provides the platform-specific filesystem paths the app needs. Each platform's implementation
 * (`AndroidPathsProvider`/`IosPathsProvider`/`JvmPathsProvider`, in `PathsProvider.<platform>.kt`) is
 * `@Singleton`-annotated and bound to this interface automatically by `CoreScanModule`'s `@ComponentScan`.
 */
interface PathsProvider {
    /** The path where all app data should be stored at. */
    val systemDataPath: Path
}

private object PathsProviderHolder : KoinComponent {
    // Deliberately NOT `by inject()`: that delegate is a Lazy cached forever on this singleton object, for the
    // whole process -- it would keep returning whichever PathsProvider was resolved *first*, even across later
    // stopKoin()/startKoin() cycles (e.g. jvmTest classes that each fake PathsProvider for their own temp dir).
    // Re-resolving on every access via get() instead makes this correctly track whichever Koin instance is
    // currently active.
    val provider: PathsProvider get() = get()
}

/**
 * Accessor for [PathsProvider] from places that can't take constructor injection (top-level
 * objects/functions). Prefer constructor injection wherever the call site is a class Koin can build.
 */
val globalPathsProvider: PathsProvider get() = PathsProviderHolder.provider
