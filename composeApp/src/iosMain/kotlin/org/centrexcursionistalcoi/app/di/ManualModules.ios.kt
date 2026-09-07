package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.platform.PlatformDragAndDrop
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { PlatformCalendarSync() }
    single { PlatformDragAndDrop() }
    single { PlatformNFC() }
    single { PlatformOpenFileLogic() }
    single { PlatformShareLogic() }
    single { BackgroundJobCoordinator(get()) }
}
