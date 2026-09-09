package org.centrexcursionistalcoi.app.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Bound service exposing [AccountAuthenticator], required for [ACCOUNT_TYPE] to be a valid `AccountManager` account type. */
class AccountAuthenticatorService : Service() {
    override fun onBind(intent: Intent?): IBinder = AccountAuthenticator(this).iBinder
}
