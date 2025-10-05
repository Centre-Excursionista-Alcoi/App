package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.response.ProfileResponse

object ProfileRemoteRepository {
    private val httpClient by lazy { getHttpClient() }

    suspend fun getProfile(): ProfileResponse? {
        val response = httpClient.get("/profile")
        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            return json.decodeFromString(ProfileResponse.serializer(), body)
        } else {
            return null
        }
    }

    suspend fun signUpForLending(
        fullName: String,
        nif: String,
        phoneNumber: String,
        sports: List<Sports>,
        address: String,
        postalCode: String,
        city: String,
        province: String,
        country: String
    ) {
        val response = httpClient.submitForm(
            "/profile/lendingSignUp",
            formParameters = parameters {
                append("fullName", fullName)
                append("nif", nif)
                append("phoneNumber", phoneNumber)
                append("sports", sports.joinToString(",") { it.name })
                append("address", address)
                append("postalCode", postalCode)
                append("city", city)
                append("province", province)
                append("country", country)
            }
        )
        if (!response.status.isSuccess()) {
            throw ServerException("Failed to sign up for lending", response.status.value, response.bodyAsText())
        }
    }

    suspend fun synchronize(): Boolean {
        val profile = getProfile()
        if (profile != null) {
            Napier.d { "User is logged in, updating cached profile data..." }
            ProfileRepository.update(profile)
            return true
        } else {
            Napier.i { "User is not logged in" }
            ProfileRepository.clear()
            return false
        }
    }
}
