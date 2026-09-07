package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.getDatabaseBuilder
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformDatabaseModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder(), get<DispatcherProvider>().io) }
}
