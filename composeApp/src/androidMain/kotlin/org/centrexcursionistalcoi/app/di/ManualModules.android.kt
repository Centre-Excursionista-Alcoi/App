package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.platform.*
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PathsProvider> { AndroidPathsProvider(androidContext()) }
    single { PlatformCalendarSync(androidContext()) }
    single { PlatformDragAndDrop(androidContext(), get()) }
    single { PlatformNFC(androidContext()) }
    single { PlatformOpenFileLogic(androidContext(), get()) }
    single { PlatformShareLogic(androidContext(), get()) }
    single { BackgroundJobCoordinator(androidContext()) }
    single { CredentialsStore(androidContext()) }
}
