package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.platform.PlatformDragAndDrop
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { PlatformCalendarSync(androidContext()) }
    single { PlatformDragAndDrop(androidContext()) }
    single { PlatformNFC(androidContext()) }
    single { PlatformOpenFileLogic(androidContext()) }
    single { PlatformShareLogic(androidContext()) }
    single { BackgroundJobCoordinator(androidContext()) }
    single { CredentialsStore(androidContext()) }
}
