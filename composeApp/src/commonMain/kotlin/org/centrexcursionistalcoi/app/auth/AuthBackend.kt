package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.centrexcursionistalcoi.app.database.*
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings

object AuthBackend {
    private val log = logging()
    
    suspend fun register(email: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Registration successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun login(email: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/login",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Login successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    @Suppress("KNOWN_EXCEPTION") // suppress because order is correct, and there won't be missing references
    suspend fun logout() {
        val response = getHttpClient().get("/logout")
        if (response.status.isSuccess()) {
            log.d { "Logged out. Removing all data..." }
            // order is important due to foreign key constraints
            LendingsRepository.deleteAll()
            InventoryItemsRepository.deleteAll()
            InventoryItemTypesRepository.deleteAll()
            EventsRepository.deleteAll()
            PostsRepository.deleteAll()
            MembersRepository.deleteAll()
            UsersRepository.deleteAll()
            DepartmentsRepository.deleteAll()
            log.d { "Removing all files..." }
            FileSystem.deleteAll().also { log.v { "$it files were deleted." } }
            log.d { "Revoking FCM token..." }
            FCMTokenManager.revoke()
            log.d { "Removing all settings..." }
            settings.clear()
        } else {
            val error = response.bodyAsError()
            log.d { "Logout failed (${response.status}): $error" }
            throw error.toThrowable()
        }
    }

    suspend fun forgotPassword(email: String) {
        val response = getHttpClient().submitForm(
            url = "/lost_password",
            formParameters = parameters {
                append("email", email)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Forgot password request successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun deleteAccount() {
        val response = getHttpClient().post("/delete_account")
        if (response.status.isSuccess()) {
            log.w { "Account delete request successful." }
            log.w { "Account deleted from server. Removing all data..." }
            // order is important due to foreign key constraints
            LendingsRepository.deleteAll()
            InventoryItemsRepository.deleteAll()
            InventoryItemTypesRepository.deleteAll()
            EventsRepository.deleteAll()
            PostsRepository.deleteAll()
            MembersRepository.deleteAll()
            UsersRepository.deleteAll()
            DepartmentsRepository.deleteAll()
            log.w { "Removing all files..." }
            FileSystem.deleteAll().also { log.v { "$it files were deleted." } }
            log.w { "Revoking FCM token..." }
            FCMTokenManager.revoke()
            log.w { "Removing all settings..." }
            settings.clear()
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }
}
