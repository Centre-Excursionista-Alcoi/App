package org.centrexcursionistalcoi.app.network

import io.ktor.client.request.post
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier

object UsersRemoteRepository : SymmetricRemoteRepository<String, UserData>(
    "/users",
    UserData.serializer(),
    UsersRepository
) {
    suspend fun promote(sub: String, progressNotifier: ProgressNotifier? = null) {
        val response = httpClient.post("/users/$sub/promote") {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsError()
            throw error.toThrowable()
        }
        // return without errors
    }
}
