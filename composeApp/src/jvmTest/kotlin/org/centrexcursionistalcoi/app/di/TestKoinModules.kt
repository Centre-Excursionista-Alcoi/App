package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.database.getDatabaseBuilder
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MembersRemoteRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.platform.PlatformDragAndDrop
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.viewmodel.LendingDetailsModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

/**
 * Verifies the whole annotation-driven Koin graph (repositories, remote repositories, services) actually resolves
 * at runtime -- compiling clean isn't enough here, since a constructor-order mismatch between an `@Singleton` class
 * and what `@ComponentScan` derives from it would only surface as a Koin runtime error, not a compile error.
 *
 * Note: `compileSafety = false` is set in `composeApp/build.gradle.kts`'s `koinCompiler { }` block because the
 * plugin's compile-time graph verification currently misfires as a false positive on this project (reports these
 * exact classes as missing even though they resolve correctly, as this test proves).
 */
class TestKoinModules {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `annotated repositories, remote repositories and services resolve through ComponentScan`() {
        val koin = startKoin {
            modules(
                module { single<AppDatabase> { getRoomDatabase(getDatabaseBuilder(), get<DispatcherProvider>().io) } },
                CoreScanModule().module(),
                PlatformScanModule().module(),
                RepositoryScanModule().module(),
                RemoteRepositoryScanModule().module(),
                ServiceScanModule().module(),
                ViewModelScanModule().module(),
            )
        }.koin

        assertNotNull(koin.get<DepartmentsRepository>())
        assertNotNull(koin.get<MembersRepository>())
        assertNotNull(koin.get<UsersRepository>())
        assertNotNull(koin.get<InventoryItemTypesRepository>())
        assertNotNull(koin.get<InventoryItemsRepository>())
        assertNotNull(koin.get<PostsRepository>())
        assertNotNull(koin.get<EventsRepository>())
        assertNotNull(koin.get<MemoriesRepository>())
        assertNotNull(koin.get<LendingsRepository>())

        assertNotNull(koin.get<DepartmentsRemoteRepository>())
        assertNotNull(koin.get<UsersRemoteRepository>())
        assertNotNull(koin.get<MembersRemoteRepository>())
        assertNotNull(koin.get<InventoryItemTypesRemoteRepository>())
        assertNotNull(koin.get<InventoryItemsRemoteRepository>())
        assertNotNull(koin.get<PostsRemoteRepository>())
        assertNotNull(koin.get<EventsRemoteRepository>())
        assertNotNull(koin.get<MemoriesRemoteRepository>())
        assertNotNull(koin.get<LendingsRemoteRepository>())

        assertNotNull(koin.get<AuthBackend>())

        assertNotNull(koin.get<PlatformNFC>())
        assertNotNull(koin.get<PlatformShareLogic>())
        assertNotNull(koin.get<PlatformOpenFileLogic>())
        assertNotNull(koin.get<PlatformCalendarSync>())
        assertNotNull(koin.get<PlatformDragAndDrop>())

        // @KoinViewModel with no runtime params
        assertNotNull(koin.get<LoginViewModel>())

        // @KoinViewModel with an @InjectedParam runtime param
        assertNotNull(koin.get<LendingDetailsModel> { parametersOf(Uuid.random()) })
    }
}
