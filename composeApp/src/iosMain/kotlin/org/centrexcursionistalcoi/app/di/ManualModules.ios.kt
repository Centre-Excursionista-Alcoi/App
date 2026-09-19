package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.platform.*
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PathsProvider> { IosPathsProvider() }
    single { PlatformCalendarSync() }
    single { PlatformDragAndDrop() }
    single { PlatformNFC() }
    single { PlatformOpenFileLogic() }
    single { PlatformShareLogic(get()) }
    single { BackgroundJobCoordinator(get()) }
    single { CredentialsStore() }
}
