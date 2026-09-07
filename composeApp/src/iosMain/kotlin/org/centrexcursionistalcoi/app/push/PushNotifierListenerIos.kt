package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
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
 * Forwards a `KMPNotifier.setLogger` message to the shared km-logging pipeline, for Swift to pass as the closure
 * body -- mirrors Android's and Desktop's `KMPNotifier.setLogger { log.d(tag = "NotifierManager") { message } }`.
 */
fun notifierManagerLog(message: String) {
    log.d(tag = "NotifierManager") { message }
}
