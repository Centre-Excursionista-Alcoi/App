package org.centrexcursionistalcoi.app.data

import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.UserNotFoundException

/**
 * Gets a [UserData] from a list by its [sub].
 *
 * If the logged-in user is not an admin, there won't be any user in the database, so this function will always throw.
 * Instead, it will return a [StubUser] to avoid crashes.
 * @throws UserNotFoundException if no user with the given [sub] is found
 */
fun List<UserData>.getUser(sub: String): UserData {
    val user = this.firstOrNull { it.sub == sub }
    if (user != null) return user

    val profile = ProfileRepository.getProfile()
    val isAdmin = profile?.isAdmin
    if (isAdmin == false) return StubUser

    throw UserNotFoundException(sub)
}

/**
 * A stub [UserData] representing an unknown user.
 *
 * Not an actual user, and only intended to be used for non-admin users.
 *
 * When the logged-in user is an admin, **this stub should never be used**, and as such, it will throw
 * [IllegalStateException] if accessed in that context.
 */
val StubUser: UserData get() {
    val profile = ProfileRepository.getProfile()
    val isAdmin = profile?.isAdmin

    if (isAdmin == true) throw IllegalStateException("Admins should never use the StubUser.")

    return UserData(
        sub = "unknown",
        memberNumber = 0u,
        fullName = "Unknown User",
        email = "unknown@example.com",
        groups = emptyList(),
        departments = emptyList(),
        lendingUser = null,
        insurances = emptyList(),
        isDisabled = false
    )
}

fun UserData.isStub(): Boolean = this.sub == "unknown"
