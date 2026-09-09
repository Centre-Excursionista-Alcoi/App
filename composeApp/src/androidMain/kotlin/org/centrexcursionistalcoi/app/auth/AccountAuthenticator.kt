package org.centrexcursionistalcoi.app.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.android.MainActivity
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Account type this app registers with [AccountManager], used to store credentials for [AuthBackend.tryAutoRelogin]. */
const val ACCOUNT_TYPE = "org.centrexcursionistalcoi.app"

/**
 * [AbstractAccountAuthenticator] for [ACCOUNT_TYPE]. Being registered as the authenticator is required for
 * [AccountManager.addAccountExplicitly] (used by [CredentialsStore]) to work at all, and every callback here
 * returns a contract-compliant [Bundle] rather than `null` -- per the `AbstractAccountAuthenticator` docs,
 * returning `null` risks a `NullPointerException` on the caller's side (Android Settings, a sync adapter, etc.).
 *
 * [confirmCredentials], [getAuthToken] and [updateCredentials] all need to call [AuthBackend.login] /
 * [AuthBackend.tryAutoRelogin], which in turn touch [CredentialsStore]'s own `AccountManager` calls
 * (`getAccountsByType`/`getPassword`/`addAccountExplicitly`). Doing that *synchronously* inside the method
 * `AccountManagerService` itself is currently calling turned out to silently fail on-device: confirmed while
 * testing [addAccount] (which originally did the equivalent `getAccountsByType` check inline) -- the request
 * to `MainActivity` never launched and no exception was ever logged anywhere. Per the `AbstractAccountAuthenticator`
 * docs, the correct pattern for any authenticator method that needs to do real work is to return `null`
 * immediately and deliver the actual result later via [AccountAuthenticatorResponse.onResult], off the
 * original call -- which is what all three do here.
 */
class AccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context), KoinComponent {
    private val authBackend: AuthBackend by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val scope by lazy { CoroutineScope(dispatcherProvider.io) }

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle =
        throw UnsupportedOperationException()

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?,
    ): Bundle {
        // No dedicated "add account" UI: launch the app itself, which shows the login screen whenever there's
        // no active session. A successful in-app login creates the account as a side effect of
        // AuthBackend.login() (see CredentialsStore). This doesn't complete the AccountAuthenticatorResponse
        // itself -- that would need deeper wiring into the shared Compose nav graph across platforms, more
        // scope than this fix covers -- so system UI driving this (e.g. Settings > Accounts > Add account)
        // won't get a result callback, but the account still ends up created.
        //
        // Deliberately doesn't check for an existing account first (even though only one is ever meant to
        // exist, see CredentialsStore): calling back into AccountManager (e.g. getAccountsByType) from here,
        // synchronously, is exactly the pattern that silently broke this method during testing -- see the
        // class doc. AuthBackend.login()'s own cleanup (via CredentialsStore.save()) already removes any
        // stale duplicate the next time a real login happens, so the worst case is a harmless leftover entry.
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, intent) }
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle? {
        val password = options?.getString(AccountManager.KEY_PASSWORD)
        if (account == null || password == null) {
            // Nothing to verify against, and no dedicated UI to launch for entering one -- see addAccount.
            return unsupported()
        }
        // Reuses AuthBackend.login() as-is, which means this also replaces any currently-active session and
        // local data on success (see AuthBackend.kt) -- acceptable since nothing in this app currently
        // triggers this callback outside of a deliberate, user-initiated system credential check.
        scope.launch {
            val verified = try {
                authBackend.login(account.name, password)
                true
            } catch (_: Exception) {
                false
            }
            response?.onResult(Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified) })
        }
        return null
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?,
    ): Bundle? {
        if (account == null) return unsupported()
        // This app's sessions are cookie-based (see SettingsCookiesStorage), not bearer tokens, so there's no
        // real "auth token" to hand back -- but a fresh session is obtained the same way as any other silent
        // relogin (see AuthBackend.tryAutoRelogin, used internally on session expiry), reusing the credentials
        // saved from the last successful login. A successful refresh is itself the meaningful result here.
        scope.launch {
            val refreshed = try {
                authBackend.tryAutoRelogin()
            } catch (_: Exception) {
                false
            }
            val result = if (refreshed) {
                Bundle().apply {
                    putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                    putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE)
                    putString(AccountManager.KEY_AUTHTOKEN, "")
                }
            } else {
                unsupported()
            }
            response?.onResult(result)
        }
        return null
    }

    override fun getAuthTokenLabel(authTokenType: String?): String = "Session"

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?,
    ): Bundle? {
        val password = options?.getString(AccountManager.KEY_PASSWORD)
        if (account == null || password == null) return unsupported()
        // Same as confirmCredentials: AuthBackend.login() both verifies the new password against the server
        // and persists it via CredentialsStore (see AuthBackend.kt) -- exactly "updating credentials".
        scope.launch {
            val updated = try {
                authBackend.login(account.name, password)
                true
            } catch (_: Exception) {
                false
            }
            val result = if (updated) {
                Bundle().apply {
                    putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                    putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE)
                }
            } else {
                unsupported()
            }
            response?.onResult(result)
        }
        return null
    }

    // No custom account features exist for ACCOUNT_TYPE, independent of getAuthToken/updateCredentials above,
    // so always reporting "unsupported" here is already correct.
    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle =
        Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }

    private fun unsupported(): Bundle = Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "Not supported.")
    }
}
