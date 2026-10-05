package org.centrexcursionistalcoi.app

import org.centrexcursionistalcoi.app.response.ProfileResponse

/**
 * Features that are still being built, and who gets to see them.
 *
 * Each flag decides from the user's [ProfileResponse]. To release a feature, make its rule `true` (and, once it has
 * been out for a while, delete the flag and its checks). This only hides the feature in the app: the server's
 * endpoints are not behind the flags.
 */
enum class FeatureFlag(private val rule: (ProfileResponse) -> Boolean) {
    /** Spaces that can be lent, their lendings, and their management. Work in progress: admins only. */
    SPACES({ it.isAdmin }),
    ;

    fun isEnabledFor(profile: ProfileResponse): Boolean = rule(profile)
}

/** Whether [flag] is enabled for this user. */
fun ProfileResponse.hasFeature(flag: FeatureFlag): Boolean = flag.isEnabledFor(this)
