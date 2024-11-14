package org.centrexcursionistalcoi.app.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.R
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.network.AuthBackend

class Authenticator(private val context: Context): AbstractAccountAuthenticator(context) {
    private val am = AccountManager.get(context)

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle?
    ): Bundle {
        val email = account.name
        val password = am.getPassword(account)

        try {
            val (token, expiration) = runBlocking { AuthBackend.login(email, password) }
            return bundleOf(
                AccountManager.KEY_ACCOUNT_NAME to account.name,
                AccountManager.KEY_ACCOUNT_TYPE to account.type,
                AccountManager.KEY_AUTHTOKEN to token,
                KEY_CUSTOM_TOKEN_EXPIRY to expiration
            )
        } catch (e: ServerException) {
            Napier.e(e) { "Could not get auth token." }
            return bundleOf(
                AccountManager.KEY_ERROR_CODE to e.code,
                AccountManager.KEY_ERROR_MESSAGE to e.message
            )
        } catch (e: NullPointerException) {
            Napier.e(e) { "Could not get auth token: Server didn't return a token." }
            return bundleOf(
                AccountManager.KEY_ERROR_CODE to -1,
                AccountManager.KEY_ERROR_MESSAGE to "Server didn't return a token"
            )
        }
    }

    override fun getAuthTokenLabel(authTokenType: String?): String {
        return context.getString(R.string.app_name)
    }

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle {
        throw UnsupportedOperationException()
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun isCredentialsUpdateSuggested(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        statusToken: String?
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        throw UnsupportedOperationException()
    }
}

