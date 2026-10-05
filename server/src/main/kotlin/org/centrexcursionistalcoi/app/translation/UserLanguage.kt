package org.centrexcursionistalcoi.app.translation

import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.acceptLanguageItems
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import org.slf4j.LoggerFactory

/**
 * The preferred language of the request (its `Accept-Language` header), or `null` if it has none (or it is any).
 * Unlike [locale], which falls back to English.
 */
fun ApplicationRequest.localeOrNull(): Locale? = acceptLanguageItems()
    .firstOrNull()
    ?.let { Locale.forLanguageTag(it.value) }
    ?.takeIf { it.language.isNotEmpty() }

/**
 * Remembers the language of the users: the one of their first request that has one (or of their registration).
 */
object UserLanguage {
    private val logger = LoggerFactory.getLogger("UserLanguage")

    /** The users known to have a language, so their requests don't ask the database. */
    private val known = ConcurrentHashMap.newKeySet<String>()

    /**
     * Sets the language of the user with [sub] from [request], if they haven't one yet. It never fails: not
     * remembering the language mustn't fail the request.
     */
    fun rememberFrom(sub: String, request: ApplicationRequest) {
        if (sub in known) return
        val locale = request.localeOrNull() ?: return
        try {
            Database { UserPreferenceStore.setIfMissing(sub, UserPreferenceKey.Language, locale) }
            known += sub
        } catch (e: Exception) {
            logger.warn("Could not remember the language of $sub", e)
        }
    }

    /**
     * Forgets which users are known to have a language.
     */
    internal fun reset() = known.clear()
}
