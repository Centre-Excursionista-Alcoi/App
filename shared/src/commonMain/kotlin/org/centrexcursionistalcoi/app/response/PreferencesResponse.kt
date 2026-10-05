package org.centrexcursionistalcoi.app.response

import kotlinx.serialization.Serializable

/**
 * The preferences of the user (`GET /profile/preferences`). Those they haven't set are `null`.
 */
@Serializable
data class PreferencesResponse(
    /** The language of the user, as a BCP 47 tag (e.g. `ca` or `es-ES`). */
    val language: String? = null,
)
