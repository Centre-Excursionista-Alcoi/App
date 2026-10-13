package org.centrexcursionistalcoi.app.auth

/**
 * Whether the platform signs in with a passkey in the user's browser, because it can't use passkeys itself: desktop.
 * Phones use their own passkeys, see [Passkeys].
 */
expect object PlatformBrowserLogin {
    val isSupported: Boolean
}
