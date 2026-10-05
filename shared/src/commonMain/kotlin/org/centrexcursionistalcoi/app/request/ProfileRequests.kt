package org.centrexcursionistalcoi.app.request

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Sports

/**
 * `POST /profile/lendingSignUp` request body.
 */
@Serializable
data class LendingSignUpRequest(
    val phoneNumber: String,
    val sports: List<Sports>,
)

/**
 * `PATCH /profile/preferences` request body. The preferences that are `null` stay as they are.
 */
@Serializable
data class UpdatePreferencesRequest(
    /** The language of the user, as a BCP 47 tag (e.g. `ca` or `es-ES`). */
    val language: String? = null,
) {
    fun isEmpty() = language == null
}

/**
 * `POST /profile/insurances` request body.
 */
@Serializable
data class CreateInsuranceRequest(
    val insuranceCompany: String,
    val policyNumber: String,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    /**
     * The documents of the insurance, in order.
     */
    val documents: List<FileWithContext> = emptyList(),
) : RequestWithFiles<CreateInsuranceRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext): CreateInsuranceRequest =
        copy(documents = documents.map(transform))
}

/**
 * `POST /profile/femecvSync` request body.
 */
@Serializable
data class LinkFEMECVRequest(
    val username: String,
    val password: String,
)

/**
 * `POST /profile/fcmToken` request body.
 */
@Serializable
data class RegisterFCMTokenRequest(
    val token: String,
    val deviceId: String? = null,
)

/**
 * `DELETE /profile/fcmToken` request body.
 */
@Serializable
data class RevokeFCMTokenRequest(
    val deviceId: String,
)
