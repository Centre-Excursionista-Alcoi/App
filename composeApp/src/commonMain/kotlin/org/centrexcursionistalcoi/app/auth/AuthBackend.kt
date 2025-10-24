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
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem

object AuthBackend {
    suspend fun register(username: String, name: String, email: String, password: String): Throwable? {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("username", username)
                append("name", name)
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            Napier.d { "Registration successful." }
            return null
        } else {
            val error = response.bodyAsError()
            Napier.d { "Registration failed (${response.status}): $error" }
            return error.toThrowable()
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
            PlatformFileSystem.deleteAll()
        } else {
            val error = response.bodyAsError()
            Napier.d { "Logout failed (${response.status}): $error" }
            throw error.toThrowable()
        }
    }
}
