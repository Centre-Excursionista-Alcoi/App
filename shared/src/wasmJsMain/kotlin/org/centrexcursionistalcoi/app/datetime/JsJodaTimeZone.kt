@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.centrexcursionistalcoi.app.datetime

/**
 * kotlinx-datetime doesn't bundle the IANA time zone database on Wasm/JS -- without this, resolving a named zone
 * (e.g. `TimeZone.of("Europe/Madrid")`, as [org.centrexcursionistalcoi.app.data.ZonedDateTime] does) throws
 * `IllegalTimeZoneException: Invalid zone ID`. Loading this npm module (see the `wasmJsMain` dependency in
 * shared/build.gradle.kts) provides the missing zone rules; referencing it here forces that load to happen.
 */
@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

@Suppress("unused")
private val jsJodaTz = JsJodaTimeZoneModule
