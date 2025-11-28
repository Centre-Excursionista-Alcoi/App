package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.data.toFormData
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.InternetAccessNotAvailable
import org.centrexcursionistalcoi.app.exception.ResourceNotModifiedException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_PROFILE_SYNC
import org.centrexcursionistalcoi.app.storage.settings

object ProfileRemoteRepository {
    private val log = logging()

    private val httpClient by lazy { getHttpClient() }

    /**
     * Gets the user's profile from the server.
     * @param progressNotifier Optional notifier to monitor download progress.
     * @param ignoreIfModifiedSince If true, ignores the "If-Modified-Since" header.
     * @return The user's profile if logged in, null if not logged in.
     * @throws ResourceNotModifiedException if the profile has not changed since the last fetch.
     */
    suspend fun getProfile(progressNotifier: ProgressNotifier? = null, ignoreIfModifiedSince: Boolean = false): ProfileResponse? {
        val response = httpClient.get("/profile") {
            progressNotifier?.let { monitorDownloadProgress(it) }
            if (!ignoreIfModifiedSince) ifModifiedSince(SETTINGS_LAST_PROFILE_SYNC)
        }
        val status = response.status
        if (status == HttpStatusCode.NotModified) {
            throw ResourceNotModifiedException()
        } else if (status.isSuccess()) {
            val currentTime = Clock.System.now()
            settings.putLong(SETTINGS_LAST_PROFILE_SYNC, currentTime.toEpochMilliseconds())

            val body = response.bodyAsText()
            return json.decodeFromString(ProfileResponse.serializer(), body)
        } else {
            return null
        }
    }

    suspend fun signUpForLending(
        phoneNumber: String,
        sports: List<Sports>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        val response = httpClient.submitForm(
            "/profile/lendingSignUp",
            formParameters = parameters {
                append("phoneNumber", phoneNumber)
                append("sports", sports.joinToString(",") { it.name })
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
        document: PlatformFile?,
        progressNotifier: ProgressNotifier? = null,
    ) {
        val documentFile = document?.let { InMemoryFileAllocator.put(it) }

        val response = httpClient.submitFormWithBinaryData(
            "/profile/insurances",
            formData = mapOf(
                "insuranceCompany" to company,
                "policyNumber" to policyNumber,
                "validFrom" to validFrom.toString(),
                "validTo" to validTo.toString(),
                "document" to documentFile,
            ).toFormData()
        ) {
            monitorUploadProgress(progressNotifier)
        }
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)
    }

    /**
     * Synchronizes the user's profile with the server.
     * @param progressNotifier Optional notifier to monitor download progress.
     * @param ignoreIfModifiedSince If true, ignores the "If-Modified-Since" header.
     * @return `true` if the user is logged in and the profile was updated, `false` if not logged in.
     * @throws InternetAccessNotAvailable if there is no internet connection.
     * @throws Exception for other errors.
     */
    suspend fun synchronize(progressNotifier: ProgressNotifier? = null, ignoreIfModifiedSince: Boolean = false): Boolean {
        try {
            val profile = getProfile(progressNotifier, ignoreIfModifiedSince)
            if (profile != null) {
                log.d { "User is logged in, updating cached profile data..." }
                ProfileRepository.update(profile)
                return true
            } else {
                log.i { "User is not logged in" }
                ProfileRepository.clear()
                return false
            }
        } catch (_: ResourceNotModifiedException) {
            log.d { "Profile not modified, no update needed." }
            return true
        } catch (e: Exception) {
            if (isNoConnectionError(e)) {
                log.w { "No internet connection, cannot synchronize profile." }
                throw InternetAccessNotAvailable()
            } else {
                log.e(e) { "Error synchronizing profile" }
                throw e
            }
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
