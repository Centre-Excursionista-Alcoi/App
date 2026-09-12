package org.centrexcursionistalcoi.app.di

import org.centrexcursionistalcoi.app.android.AppBase
import org.centrexcursionistalcoi.app.android.ContextProvider
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.getDatabaseBuilder
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.koin.android.error.MissingAndroidContextException
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformDatabaseModule(): Module = module {
    single<AppDatabase> {
        // AppDatabase is normally the first single Koin resolves (LoadingViewModel needs it right away),
        // so it's the one that surfaces it if Koin's own androidContext() registration isn't visible yet for
        // some reason (seen on an automated test device, Sentry APP-ANDROID-6Q) -- fall back to the same
        // static context sources SystemDataPath.android.kt already relies on for exactly this situation.
        val context = try {
            androidContext()
        } catch (_: MissingAndroidContextException) {
            ContextProvider.context ?: AppBase.instance ?: error("Could not find any valid context")
        }
        getRoomDatabase(getDatabaseBuilder(context), get<DispatcherProvider>().io)
    }
}
