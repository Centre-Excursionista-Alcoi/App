package org.centrexcursionistalcoi.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Starts Koin with [databaseModules], the repository/remote-repository/service modules, plus any
 * additional [extraModules].
 *
 * Call this once per process, as early as possible during platform startup.
 */
fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        modules(databaseModules() + repositoryModule + remoteRepositoryModule + serviceModule + extraModules)
    }
}
