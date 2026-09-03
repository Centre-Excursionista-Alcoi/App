package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.database.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the platform-specific [AppDatabase] instance (backed by a Room database builder).
 *
 * Each platform builds this differently since obtaining a [androidx.room3.RoomDatabase.Builder]
 * requires platform-specific context (e.g. an Android [android.content.Context]).
 */
expect fun platformDatabaseModule(): Module

/**
 * Provides all the Room DAOs, sourced from the [AppDatabase] instance provided by [platformDatabaseModule].
 */
val daoModule = module {
    single { get<AppDatabase>().departmentDao() }
    single { get<AppDatabase>().eventDao() }
    single { get<AppDatabase>().inventoryItemTypeDao() }
    single { get<AppDatabase>().inventoryItemDao() }
    single { get<AppDatabase>().lendingDao() }
    single { get<AppDatabase>().lendingItemDao() }
    single { get<AppDatabase>().memberDao() }
    single { get<AppDatabase>().memoryDao() }
    single { get<AppDatabase>().postDao() }
    single { get<AppDatabase>().receivedItemDao() }
    single { get<AppDatabase>().userDao() }
}

/**
 * All the modules required to inject [AppDatabase] and its DAOs.
 */
fun databaseModules(): List<Module> = listOf(platformDatabaseModule(), daoModule)
