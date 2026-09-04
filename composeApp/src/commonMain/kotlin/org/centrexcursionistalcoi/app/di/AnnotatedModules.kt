package org.centrexcursionistalcoi.app.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Scans `database` for `@Singleton`-annotated Room repositories, which build on the DAOs provided by [daoModule]. */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.database")
class RepositoryScanModule

/** Scans `network` for `@Singleton`-annotated remote (server-backed) repositories. */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.network")
class RemoteRepositoryScanModule

/** Scans `auth` for `@Singleton`-annotated application services (e.g. [org.centrexcursionistalcoi.app.auth.AuthBackend]). */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.auth")
class ServiceScanModule

/** Scans `viewmodel` for `@KoinViewModel`-annotated view models. */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.viewmodel")
class ViewModelScanModule

/** Scans `di` for `@Singleton`-annotated core services (e.g. [DefaultDispatcherProvider]). */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.di")
class CoreScanModule

/** Scans `platform` for `@Singleton`-annotated platform providers (e.g. [org.centrexcursionistalcoi.app.platform.PlatformNFC]). */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.platform")
class PlatformScanModule

/** Scans `sync` for `@Singleton`-annotated background job logics (e.g. [org.centrexcursionistalcoi.app.sync.SyncPostBackgroundJob]). */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.sync")
class SyncScanModule

/** Scans `push` for `@Singleton`-annotated background job logics (e.g. [org.centrexcursionistalcoi.app.push.SSENotificationsListener]). */
@Module
@ComponentScan("org.centrexcursionistalcoi.app.push")
class PushScanModule
