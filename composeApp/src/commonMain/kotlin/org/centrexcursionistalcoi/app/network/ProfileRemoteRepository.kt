package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse

object ProfileRemoteRepository {
    private val httpClient by lazy { getHttpClient() }

    suspend fun getProfile(progressNotifier: ProgressNotifier? = null): ProfileResponse? {
        val response = httpClient.get("/profile") {
            progressNotifier?.let { monitorDownloadProgress(it) }
        }
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
        country: String,
        progressNotifier: ProgressNotifier? = null,
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
        ) {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (!response.status.isSuccess()) {
            throw ServerException(
                "Failed to sign up for lending",
                response.status.value,
                response.bodyAsText(),
                errorCode = response.headers["CEA-Error-Code"]?.toIntOrNull(),
            )
        }
    }

    suspend fun createInsurance(
        company: String,
        policyNumber: String,
        validFrom: LocalDate,
        validTo: LocalDate,
        progressNotifier: ProgressNotifier? = null,
    ) {
        val response = httpClient.submitForm(
            "/profile/insurances",
            formParameters = parameters {
                append("insuranceCompany", company)
                append("policyNumber", policyNumber)
                append("validFrom", validFrom.toString())
                append("validTo", validTo.toString())
            }
        ) {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)
    }

    suspend fun synchronize(progressNotifier: ProgressNotifier? = null): Boolean {
        val profile = getProfile(progressNotifier)
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

    suspend fun connectFEMECV(username: String, password: CharArray, progressNotifier: ProgressNotifier? = null) {
        val response = httpClient.submitForm(
            "/profile/femecvSync",
            formParameters = parameters {
                append("username", username)
                append("password", password.concatToString())
            }
        ) {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)

        // After connecting, synchronize the profile to update local data
        synchronize()
    }

    suspend fun disconnectFEMECV(progressNotifier: ProgressNotifier? = null) {
        val response = httpClient.delete("/profile/femecvSync") {
            progressNotifier?.let { monitorDownloadProgress(it) }
        }
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)
    }
}
