package org.centrexcursionistalcoi.app.auth

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.storage.settings

/**
 * Generates a new PKCE code verifier and challenge, stores the verifier associated with a unique state,
 * and returns the state and code challenge.
 *
 * @return A pair containing the unique state (UUID) and the corresponding code challenge (String).
 */
@OptIn(ExperimentalUuidApi::class)
fun generateAndStorePCKE(): Pair<Uuid, String> {
    val state = Uuid.random()
    val codeVerifier = generateCodeVerifier()
    val codeChallenge = generateCodeChallenge(codeVerifier)
    settings.putString("pkce.${state}", codeVerifier)
    return state to codeChallenge
}

/**
 * Retrieves and removes the PKCE code verifier associated with the given state.
 *
 * @param state The unique state (UUID) used to look up the code verifier.
 * @return The corresponding code verifier (String) if found, or null if not found.
 */
@OptIn(ExperimentalUuidApi::class)
fun retrieveAndRemoveCodeVerifier(state: Uuid): String? {
    val codeVerifier = settings.getStringOrNull("pkce.$state")
    if (codeVerifier != null) {
        settings.remove("pkce.${state}")
    }
    return codeVerifier
}
