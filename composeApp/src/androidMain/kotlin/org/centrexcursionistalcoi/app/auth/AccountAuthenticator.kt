package org.centrexcursionistalcoi.app.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Account type this app registers with [AccountManager], used to store credentials for [AuthBackend.tryAutoRelogin]. */
const val ACCOUNT_TYPE = "org.centrexcursionistalcoi.app"

/**
 * [AbstractAccountAuthenticator] for [ACCOUNT_TYPE]. Being registered as the authenticator is required for
 * [AccountManager.addAccountExplicitly] (used by [CredentialsStore]) to work at all, but this app doesn't
 * drive the OS "Add account" / "Update credentials" UI itself -- the one account it ever creates is a side
 * effect of a successful in-app login (see [CredentialsStore]). [confirmCredentials] is the one callback with
 * a natural, low-risk implementation: verifying a password against the server reuses [AuthBackend.login] as-is.
 *
 * The other callbacks return `null` were left unimplemented: per the `AbstractAccountAuthenticator` docs,
 * returning `null` (as opposed to a contract-compliant error [Bundle]) risks a `NullPointerException` on
 * the caller's side (Android Settings, a sync adapter, etc.) if anything ever actually invokes them --
 * unlikely in practice since nothing routes through the system Account UI today, but worth being correct
 * about regardless.
 */
class AccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context), KoinComponent {
    private val authBackend: AuthBackend by inject()

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle =
        throw UnsupportedOperationException()

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?,
    ): Bundle =
        // Accounts of this type are only ever created as a side effect of a successful in-app login; there's
        // no UI to drive here that would let an externally-triggered "add account" request complete the same
        // way. Revisit if this app should support the system-initiated flow (Settings > Accounts > Add
        // account), which would need MainActivity to launch into Login and complete the
        // AccountAuthenticatorResponse once that succeeds -- more scope than this fix covers.
        unsupported()

    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle {
        val password = options?.getString(AccountManager.KEY_PASSWORD)
        if (account == null || password == null) {
            // Nothing to verify against, and no dedicated UI to launch for entering one -- see addAccount.
            return unsupported()
        }
        // Reuses AuthBackend.login() as-is, which means this also replaces any currently-active session and
        // local data on success (see AuthBackend.kt) -- acceptable since nothing in this app currently
        // triggers this callback outside of a deliberate, user-initiated system credential check.
        val verified = runBlocking {
            try {
                authBackend.login(account.name, password)
                true
            } catch (_: Exception) {
                false
            }
        }
        return Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified) }
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?,
    ): Bundle =
        // This app's sessions are cookie-based (see SettingsCookiesStorage), not bearer tokens, so there's no
        // "auth token" this API's shape maps onto -- AuthBackend.tryAutoRelogin() (used internally on session
        // expiry) doesn't need or go through this callback.
        unsupported()

    override fun getAuthTokenLabel(authTokenType: String?): String? = null

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?,
    ): Bundle =
        unsupported()

    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle =
        Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }

    private fun unsupported(): Bundle = Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "Not supported.")
    }
}
