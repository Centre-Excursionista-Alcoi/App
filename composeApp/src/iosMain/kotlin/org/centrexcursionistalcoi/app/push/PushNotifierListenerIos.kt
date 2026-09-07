package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.logger.Logger
import org.koin.mp.KoinPlatformTools

/**
 * Resolves the Koin-managed [PushNotifierListener] singleton, for Swift to pass to `KMPNotifier.addPushListener`.
 *
 * [PushNotifierListener] is a regular Koin-injected class (not a Kotlin `object`), so it has no `.shared` accessor
 * on the Swift side -- this is the iOS equivalent of Android's `get<PushNotifierListener>()` in `AppBase`.
 *
 * Must be called after [org.centrexcursionistalcoi.app.di.initKoin] has run.
 */
fun pushNotifierListener(): PushNotifierListener = KoinPlatformTools.defaultContext().get().get()

private val log = logging()

/**
 * A [Logger] forwarding to the shared km-logging pipeline, for Swift to pass to `KMPNotifier.setLogger`. Built here
 * (not as a Swift closure) because [Logger] is a `fun interface` from a transitively-exported dependency
 * (`kmm-notifier-core`), and cross-module SAM conversion doesn't reach Swift through the export boundary --
 * Kotlin's own SAM conversion always works within the same compilation, so the finished object is handed over
 * instead of asking Swift to build one.
 */
fun notifierManagerLogger(): Logger = Logger { message -> log.d(tag = "NotifierManager") { message } }
