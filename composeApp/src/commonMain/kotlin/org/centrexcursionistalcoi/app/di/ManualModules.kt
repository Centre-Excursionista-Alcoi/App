package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MembersRemoteRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformSaveFileLogic
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.sync.BackgroundJob
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncDepartmentBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncEntityBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncEventBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncPostBackgroundJob
import org.centrexcursionistalcoi.app.viewmodel.ActivityMemoryEditorViewModel
import org.centrexcursionistalcoi.app.viewmodel.FileProviderModel
import org.centrexcursionistalcoi.app.viewmodel.HomePageModel
import org.centrexcursionistalcoi.app.viewmodel.InventoryItemTypeDetailsScreenModel
import org.centrexcursionistalcoi.app.viewmodel.LendingCreationViewModel
import org.centrexcursionistalcoi.app.viewmodel.LendingDetailsModel
import org.centrexcursionistalcoi.app.viewmodel.LendingManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.LendingPageModel
import org.centrexcursionistalcoi.app.viewmodel.LendingSignUpViewModel
import org.centrexcursionistalcoi.app.viewmodel.LendingsPageModel
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.centrexcursionistalcoi.app.viewmodel.LogoutViewModel
import org.centrexcursionistalcoi.app.viewmodel.MainScreenViewModel
import org.centrexcursionistalcoi.app.viewmodel.MainViewModel
import org.centrexcursionistalcoi.app.viewmodel.ManagementPageScreenModel
import org.centrexcursionistalcoi.app.viewmodel.MemoriesViewModel
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel
import org.centrexcursionistalcoi.app.viewmodel.ProfilePageModel
import org.centrexcursionistalcoi.app.viewmodel.SettingsViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.DepartmentsManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.EventsManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.InventoryManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.LendingsManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.MemoriesManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.PostsManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.management.UsersManagementViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Manual, hand-written replacement for the `@ComponentScan`-driven modules in `AnnotatedModules.kt`
 * (disabled -- see TODO on `koinCompilerPlugin` in `composeApp/build.gradle.kts` and
 * https://github.com/Centre-Excursionista-Alcoi/App/issues/590).
 *
 * Every `single`/`viewModel` here mirrors exactly what the Koin compiler plugin would have generated
 * from the `@Singleton` / `@KoinViewModel` / `@Factory` / `@Named` / `@InjectedParam` annotations that
 * are still present on these classes (left in place, inert, so this file is a drop-in revert target).
 *
 * DELETE THIS FILE and restore `AnnotatedModules.kt` usage in [initKoin] once the plugin is re-enabled.
 */
val manualModule = module {
    // di -- CoreScanModule
    single<DispatcherProvider> { DefaultDispatcherProvider() }

    // platform -- PlatformScanModule (platform-specific expect/actual providers are in platformModule())
    single { PlatformSaveFileLogic() }

    // database -- RepositoryScanModule
    single { DepartmentsRepository(get()) }
    single { EventsRepository(get()) }
    single { InventoryItemsRepository(get()) }
    single { InventoryItemTypesRepository(get()) }
    single { LendingsRepository(get(), get()) }
    single { MembersRepository(get()) }
    single { MemoriesRepository(get()) }
    single { PostsRepository(get()) }
    single { UsersRepository(get()) }

    // network -- RemoteRepositoryScanModule
    single { DepartmentsRemoteRepository(get(), get()) }
    single { EventsRemoteRepository(get()) }
    single { InventoryItemsRemoteRepository(get()) }
    single { InventoryItemTypesRemoteRepository(get()) }
    single { LendingsRemoteRepository(get(), get()) }
    single { MembersRemoteRepository(get()) }
    single { MemoriesRemoteRepository(get()) }
    single { PostsRemoteRepository(get()) }
    single { UsersRemoteRepository(get()) }

    // auth -- ServiceScanModule
    single { AuthBackend(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // sync -- SyncScanModule
    single { DatabaseIntegrityVerifier(get(), get(), get(), get(), get(), get(), get()) }
    // Also bound as BackgroundJob (in addition to each concrete type, which Koin infers from the lambda return
    // type and keeps resolvable on its own): BackgroundJobWorker looks these up via
    // `inject(BackgroundJob::class.java, named(...))`, so a definition registered only under its concrete type is
    // invisible to that lookup and every job fails immediately with NoDefinitionFoundException -- silently
    // breaking all background sync.
    single(named(SyncAllDataBackgroundJob.UNIQUE_NAME)) {
        SyncAllDataBackgroundJob(
            get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    } bind BackgroundJob::class
    single(named(SyncDepartmentBackgroundJob.NAME)) { SyncDepartmentBackgroundJob(get()) } bind BackgroundJob::class
    single(named(SyncEntityBackgroundJob.NAME)) { SyncEntityBackgroundJob() } bind BackgroundJob::class
    single(named(SyncEventBackgroundJob.NAME)) { SyncEventBackgroundJob(get()) } bind BackgroundJob::class
    single(named(SyncLendingBackgroundJob.NAME)) { SyncLendingBackgroundJob(get(), get()) } bind BackgroundJob::class
    single(named(SyncPostBackgroundJob.NAME)) { SyncPostBackgroundJob(get()) } bind BackgroundJob::class

    // push -- PushScanModule
    single { PushNotifierListener(get(), get()) }
    single { SSENotificationsListener(get(), get()) }

    // viewmodel -- ViewModelScanModule
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { LendingPageModel(get()) }
    viewModel { (params: ActivityMemoryEditorViewModel.Params) ->
        ActivityMemoryEditorViewModel(params, get(), get(), get(), get(), get(), get())
    }
    viewModel { FileProviderModel(get(), get(), get(), get()) }
    viewModel { HomePageModel(get(), get(), get()) }
    viewModel { (typeId: kotlin.uuid.Uuid) -> InventoryItemTypeDetailsScreenModel(typeId, get()) }
    viewModel { (originalShoppingList: org.centrexcursionistalcoi.app.typing.ShoppingList) ->
        LendingCreationViewModel(originalShoppingList, get(), get(), get(), get())
    }
    viewModel { (lendingId: kotlin.uuid.Uuid) -> LendingDetailsModel(lendingId, get(), get(), get()) }
    viewModel { (lendingId: kotlin.uuid.Uuid) ->
        LendingManagementViewModel(lendingId, get(), get(), get(), get(), get())
    }
    viewModel { LendingSignUpViewModel(get()) }
    viewModel { LendingsPageModel(get(), get()) }
    viewModel { LoadingViewModel(get(), get(), get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { (afterLogout: () -> Unit) -> LogoutViewModel(get(), get(), afterLogout) }
    viewModel { MainScreenViewModel(get(), get(), get(), get(), get()) }
    viewModel { ManagementPageScreenModel(get(), get()) }
    viewModel { MemoriesViewModel(get()) }
    viewModel { (url: io.ktor.http.Url?) -> PlatformInitializerViewModel(url, get(), get()) }
    viewModel { ProfilePageModel(get(), get()) }
    viewModel { (onDeleteAccount: () -> Unit) -> SettingsViewModel(get(), get(), get(), onDeleteAccount) }
    viewModel { DepartmentsManagementViewModel(get(), get(), get(), get()) }
    viewModel { EventsManagementViewModel(get(), get(), get(), get()) }
    viewModel { InventoryManagementViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { LendingsManagementViewModel(get(), get(), get(), get()) }
    viewModel { MemoriesManagementViewModel(get(), get(), get(), get(), get()) }
    viewModel { PostsManagementViewModel(get(), get(), get(), get()) }
    viewModel { UsersManagementViewModel(get(), get(), get(), get(), get(), get()) }
}

/** Platform-specific `single`s for the `expect`/`actual` providers ([BackgroundJobCoordinator], `Platform*` classes). */
expect fun platformModule(): Module
