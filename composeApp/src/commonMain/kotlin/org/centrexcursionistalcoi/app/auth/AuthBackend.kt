package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
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
    suspend fun register(nif: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("nif", nif)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            Napier.d { "Registration successful." }
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
            Napier.d { "Login successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun logout() {
        val response = getHttpClient().get("/logout")
        if (response.status.isSuccess()) {
            Napier.d { "Logged out. Removing all data..." }
            // order is important due to foreign key constraints
            LendingsRepository.deleteAll()
            UsersRepository.deleteAll()
            InventoryItemsRepository.deleteAll()
            InventoryItemTypesRepository.deleteAll()
            PostsRepository.deleteAll()
            DepartmentsRepository.deleteAll()
            Napier.d { "Removing all files..." }
            FileSystem.deleteAll()
            Napier.d { "Revoking FCM token..." }
            FCMTokenManager.revoke()
            Napier.d { "Removing all settings..." }
            settings.clear()
        } else {
            val error = response.bodyAsError()
            Napier.d { "Logout failed (${response.status}): $error" }
            throw error.toThrowable()
        }
    }
}
