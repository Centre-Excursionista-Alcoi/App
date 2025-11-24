package org.centrexcursionistalcoi.app.translation

import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.acceptLanguageItems
import java.util.Locale

/**
 * Retrieves the preferred locale from the request's `Accept-Language` header.
 * If no valid locale is found, defaults to English.
 *
 * @receiver The incoming application request.
 * @return The preferred locale.
 */
fun ApplicationRequest.locale(): Locale {
    return call.request.acceptLanguageItems().firstOrNull()
        ?.let { Locale.forLanguageTag(it.value) }
        ?: Locale.ENGLISH
}
