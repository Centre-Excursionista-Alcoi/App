package org.centrexcursionistalcoi.app.database

import java.util.Locale

/**
 * A preference a user can have, with the type of its value: stored as text, and read back from it.
 *
 * Preferences don't have to be declared to be stored (see [UserPreferenceStore]); declaring one is for the ones the
 * server uses, so it reads them with their type.
 */
class UserPreferenceKey<T : Any>(
    /** What the preference is called in the database. */
    val name: String,
    val encode: (T) -> String,
    /** `null` if the text isn't a valid value (the preference is then as if it wasn't set). */
    val decode: (String) -> T?,
) {
    companion object {
        /**
         * The language of the user, the one they use the app in. It decides the language of the emails sent to them.
         */
        val Language = UserPreferenceKey<Locale>(
            name = "language",
            encode = Locale::toLanguageTag,
            decode = { Locale.forLanguageTag(it).takeIf { locale -> locale.language.isNotEmpty() } },
        )
    }
}
