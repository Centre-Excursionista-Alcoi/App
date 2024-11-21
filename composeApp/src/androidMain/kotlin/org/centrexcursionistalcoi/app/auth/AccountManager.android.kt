package org.centrexcursionistalcoi.app.auth

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import java.io.Closeable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.push.PushNotifications
import android.accounts.Account as AndroidAccount

actual object AccountManager : Closeable {
    private const val ACCOUNT_TYPE = "org.centrexcursionistalcoi.app"

    private lateinit var am: AccountManager

    private val accounts = MutableStateFlow<Array<AndroidAccount>?>(null)

    private val listener: OnAccountsUpdateListener = OnAccountsUpdateListener {
        accounts.tryEmit(it)
    }

    private fun AndroidAccount.toAccount(): Account = Account(name)

    private fun Account.toAndroidAccount(): AndroidAccount = AndroidAccount(email, ACCOUNT_TYPE)

    fun initialize(context: Context) {
        am = AccountManager.get(context)

        am.addOnAccountsUpdatedListener(
            listener,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Handler.createAsync(context.mainLooper)
            } else {
                Handler(context.mainLooper)
            },
            true
        )
    }

    override fun close() {
        am.removeOnAccountsUpdatedListener(listener)
    }

    actual suspend fun get(): AccountAndPassword? {
        val account = am.getAccountsByType(ACCOUNT_TYPE).firstOrNull() ?: return null
        val password = am.getPassword(account)

        return AccountAndPassword(account.toAccount(), password)
    }

    actual fun flow(): Flow<AccountAndPassword?> {
        return accounts
            .asStateFlow()
            .map { accounts ->
                accounts?.firstOrNull()?.let { account ->
                    val password = am.getPassword(account)
                    AccountAndPassword(account.toAccount(), password)
                }
            }
    }

    actual suspend fun put(account: Account, password: String) {
        am.addAccountExplicitly(account.toAndroidAccount(), password, Bundle())
        PushNotifications.refreshTokenOnServer()
    }

    actual suspend fun logout() {
        val account = am.getAccountsByType(ACCOUNT_TYPE).firstOrNull() ?: return

        am.removeAccountExplicitly(account)
    }
}
