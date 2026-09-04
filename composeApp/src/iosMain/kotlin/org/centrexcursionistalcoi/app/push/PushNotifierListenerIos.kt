package org.centrexcursionistalcoi.app.push

import org.koin.mp.KoinPlatformTools

/**
 * Resolves the Koin-managed [PushNotifierListener] singleton, for Swift to pass to `NotifierManager.addListener`.
 *
 * [PushNotifierListener] is a regular Koin-injected class (not a Kotlin `object`), so it has no `.shared` accessor
 * on the Swift side -- this is the iOS equivalent of Android's `get<PushNotifierListener>()` in `AppBase`.
 *
 * Must be called after [org.centrexcursionistalcoi.app.di.initKoin] has run.
 */
fun pushNotifierListener(): PushNotifierListener = KoinPlatformTools.defaultContext().get().get()
