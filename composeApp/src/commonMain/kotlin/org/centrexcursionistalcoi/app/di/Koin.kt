package org.centrexcursionistalcoi.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Starts Koin with [databaseModules] (manual DSL for [org.centrexcursionistalcoi.app.database.AppDatabase] and its
 * DAOs, since platform-specific construction doesn't fit component scanning) plus the annotation-driven modules
 * (repositories, remote repositories, services, view models -- see [AnnotatedModules.kt]), plus any additional
 * [extraModules].
 *
 * Call this once per process, as early as possible during platform startup.
 */
fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        modules(
            databaseModules() +
                RepositoryScanModule().module() +
                RemoteRepositoryScanModule().module() +
                ServiceScanModule().module() +
                ViewModelScanModule().module() +
                extraModules,
        )
    }
}
