package org.centrexcursionistalcoi.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Starts Koin with [databaseModules] plus any additional [extraModules].
 *
 * Call this once per process, as early as possible during platform startup.
 */
fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        modules(databaseModules() + extraModules)
    }
}
