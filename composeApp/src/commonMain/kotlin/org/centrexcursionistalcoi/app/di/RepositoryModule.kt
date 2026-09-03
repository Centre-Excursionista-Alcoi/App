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
import org.koin.dsl.module

/**
 * Provides the Room-backed repositories, wired to the DAOs from [daoModule] and to each other for
 * cross-referencing (e.g. [EventsRepository] needs [DepartmentsRepository] and [UsersRepository] to
 * resolve a [org.centrexcursionistalcoi.app.data.ReferencedEvent]).
 */
val repositoryModule = module {
    single { DepartmentsRepository(get()) }
    single { MembersRepository(get()) }
    single { UsersRepository(get()) }
    single { InventoryItemTypesRepository(get(), get()) }
    single { InventoryItemsRepository(get(), get()) }
    single { PostsRepository(get(), get()) }
    single { EventsRepository(get(), get(), get()) }
    single { MemoriesRepository(get(), get(), get(), get()) }
    single { LendingsRepository(get(), get(), get(), get()) }
}

/**
 * Provides the remote (server-backed) repositories, each wrapping the local repository it syncs plus
 * whatever other repositories it needs to resolve cross-referenced data.
 */
val remoteRepositoryModule = module {
    single { DepartmentsRemoteRepository(get(), get()) }
    single { UsersRemoteRepository(get()) }
    single { MembersRemoteRepository(get()) }
    single { InventoryItemTypesRemoteRepository(get(), get()) }
    single { InventoryItemsRemoteRepository(get(), get()) }
    single { PostsRemoteRepository(get(), get()) }
    single { EventsRemoteRepository(get(), get(), get()) }
    single { MemoriesRemoteRepository(get(), get(), get(), get()) }
    single { LendingsRemoteRepository(get(), get(), get(), get(), get(), get(), get()) }
}

/** Provides application services that depend on the repositories above. */
val serviceModule = module {
    single { AuthBackend(get(), get(), get(), get(), get(), get(), get(), get()) }
}
