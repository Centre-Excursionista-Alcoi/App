package org.centrexcursionistalcoi.app.viewmodel

import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException

/** What went wrong signing in or registering, in terms a screen can word (the screens own the wording). */
enum class AuthErrorKind {
    USER_NOT_REGISTERED,
    INVALID_CREDENTIALS,
    INVALID_CODE,
    EMAIL_NOT_FOUND,
    MEMBER_NOT_ACTIVE,
    ALREADY_REGISTERED,
    PASSWORD_NOT_SAFE,
    PASSWORD_NOT_SET,
    BROWSER_EXPIRED,
    PASSKEY,
    UNKNOWN,
}

/** @param detail The error's own message, for the kinds that show it ([AuthErrorKind.PASSKEY], [AuthErrorKind.UNKNOWN]). */
data class AuthError(val kind: AuthErrorKind, val detail: String?)

fun Throwable.toAuthError(): AuthError = when {
    this is ServerException -> AuthError(
        kind = when (errorCode) {
            Error.ERROR_USER_NOT_REGISTERED -> AuthErrorKind.USER_NOT_REGISTERED
            Error.ERROR_INCORRECT_PASSWORD_OR_EMAIL -> AuthErrorKind.INVALID_CREDENTIALS
            Error.ERROR_INVALID_VERIFICATION_CODE -> AuthErrorKind.INVALID_CODE
            Error.ERROR_EMAIL_NOT_FOUND -> AuthErrorKind.EMAIL_NOT_FOUND
            Error.ERROR_MEMBER_IS_NOT_ACTIVE -> AuthErrorKind.MEMBER_NOT_ACTIVE
            Error.ERROR_USER_ALREADY_REGISTERED -> AuthErrorKind.ALREADY_REGISTERED
            Error.ERROR_PASSWORD_NOT_SAFE_ENOUGH -> AuthErrorKind.PASSWORD_NOT_SAFE
            Error.ERROR_PASSWORD_NOT_SET -> AuthErrorKind.PASSWORD_NOT_SET
            else -> AuthErrorKind.UNKNOWN
        },
        detail = message,
    )
    this is BrowserLoginExpiredException -> AuthError(AuthErrorKind.BROWSER_EXPIRED, message)
    this is PasskeyException -> AuthError(AuthErrorKind.PASSKEY, message)
    else -> AuthError(AuthErrorKind.UNKNOWN, toString())
}
