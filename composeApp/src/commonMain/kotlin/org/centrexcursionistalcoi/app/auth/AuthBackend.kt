package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings

object AuthBackend {
    private val log = logging()
    
    suspend fun register(nif: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("nif", nif)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Registration successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun login(nif: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/login",
            formParameters = parameters {
                append("nif", nif)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Login successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun logout() {
        val response = getHttpClient().get("/logout")
        if (response.status.isSuccess()) {
            log.d { "Logged out. Removing all data..." }
            // order is important due to foreign key constraints
            LendingsRepository.deleteAll()
            InventoryItemsRepository.deleteAll()
            InventoryItemTypesRepository.deleteAll()
            PostsRepository.deleteAll()
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

    suspend fun forgotPassword(nif: String) {
        val response = getHttpClient().submitForm(
            url = "/lost_password",
            formParameters = parameters {
                append("nif", nif)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Forgot password request successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }
}
