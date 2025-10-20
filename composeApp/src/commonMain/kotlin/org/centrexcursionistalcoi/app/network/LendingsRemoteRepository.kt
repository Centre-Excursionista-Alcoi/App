package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json

object LendingsRemoteRepository : RemoteRepository<Uuid, Lending>(
    "/inventory/lendings",
    Lending.serializer(),
    LendingsRepository
) {
    suspend fun create(from: LocalDate, to: LocalDate, itemsIds: List<Uuid>, notes: String? = null) {
        val response = httpClient.submitForm("inventory/lendings", parameters {
            append("from", from.toString())
            append("to", to.toString())
            append("items", itemsIds.joinToString(","))
            if (notes != null) {
                append("notes", notes)
            }
        })
        if (response.status.isSuccess()) {
            val location = response.headers["Location"]
                ?: throw IllegalArgumentException("Missing Location header in response")
            val id = location.substringAfterLast("/").let { Uuid.parse(it) }
            val lending = get(id) ?: throw NoSuchElementException("Lending $id not found after creation")
            LendingsRepository.insert(lending)
        } else {
            throw IllegalArgumentException("Failed to create lending (${response.status}): ${response.bodyAsText()}")
        }
    }

    /**
     * Requests the server for available items of a given type within a date range, and allocates the specified amount if available.
     * @param typeId The UUID of the inventory item type to allocate.
     * @param from The start date of the allocation period.
     * @param to The end date of the allocation period.
     * @param amount The number of items to allocate.
     * @return A list of UUIDs representing the allocated inventory items.
     * @throws CannotAllocateEnoughItemsException if there are not enough items available to allocate.
     * @throws IllegalArgumentException for other allocation failures.
     */
    suspend fun allocate(typeId: Uuid, from: LocalDate, to: LocalDate, amount: Int): List<Uuid> {
        require(amount > 0) { "Amount must be greater than zero" }

        val response = httpClient.get("inventory/types/$typeId/allocate") {
            parameter("from", from.toString())
            parameter("to", to.toString())
            parameter("amount", amount)
        }
        if (response.status.isSuccess()) {
            return response.bodyAsText().let { body ->
                json.decodeFromString(ListSerializer(Uuid.serializer()), body)
            }
        } else {
            when (response.status) {
                HttpStatusCode.Conflict -> {
                    val availableItemIds = response.headers["CEA-Available-Items"]
                        ?.split(',')
                        ?.map { Uuid.parse(it) }
                    throw CannotAllocateEnoughItemsException(availableItemIds, amount)
                }
                else -> {
                    throw IllegalArgumentException("Failed to allocate items (${response.status}): ${response.bodyAsText()}")
                }
            }
        }
    }

    /**
     * Cancels a lending request by its ID.
     * The logged-in user must be the owner of the lending.
     * The lending must not have been picked up yet.
     * @param lendingId The UUID of the lending to cancel.
     * @throws IllegalArgumentException if the cancellation fails.
     */
    suspend fun cancel(lendingId: Uuid) {
        val response = httpClient.post("inventory/lendings/$lendingId/cancel")
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Failed to cancel lending (${response.status}): ${response.bodyAsText()}")
        }
        LendingsRepository.delete(lendingId)
    }

    /**
     * Confirms a lending request by its ID.
     * The logged-in user must have the necessary permissions to confirm lendings.
     * @param lendingId The UUID of the lending to confirm.
     * @throws IllegalArgumentException if the confirmation fails.
     * @throws NoSuchElementException if the lending is not found after confirmation.
     */
    suspend fun confirm(lendingId: Uuid) {
        val response = httpClient.post("inventory/lendings/$lendingId/confirm")
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Failed to confirm lending (${response.status}): ${response.bodyAsText()}")
        }
        val updatedLending = get(lendingId) ?: throw NoSuchElementException("Lending $lendingId not found after confirmation")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Marks a lending as picked up by its ID.
     * The logged-in user must have the necessary permissions to pickup lendings.
     * @param lendingId The UUID of the lending to pickup.
     * @throws IllegalArgumentException if the pickup fails.
     * @throws NoSuchElementException if the lending is not found after pickup.
     */
    suspend fun pickup(lendingId: Uuid) {
        val response = httpClient.post("inventory/lendings/$lendingId/pickup")
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Failed to pickup lending (${response.status}): ${response.bodyAsText()}")
        }
        val updatedLending = get(lendingId) ?: throw NoSuchElementException("Lending $lendingId not found after pickup")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Marks a lending as returned by its ID.
     * The logged-in user must have the necessary permissions to receive lendings.
     * @param lendingId The UUID of the lending to receive.
     * @throws IllegalArgumentException if the receive fails.
     * @throws NoSuchElementException if the lending is not found after receive.
     */
    suspend fun `return`(lendingId: Uuid) {
        val response = httpClient.post("inventory/lendings/$lendingId/return")
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("Failed to return lending (${response.status}): ${response.bodyAsText()}")
        }
        val updatedLending = get(lendingId) ?: throw NoSuchElementException("Lending $lendingId not found after return")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Submits a memory file for a lending by its ID.
     * The logged-in user must be the owner of the lending.
     * @param lendingId The UUID of the lending to submit the memory for.
     * @param file The memory file to submit.
     * @param progress An optional progress listener for upload progress.
     * @throws ServerException if the submission fails.
     * @throws NoSuchElementException if the lending is not found after submission.
     */
    suspend fun submitMemory(lendingId: Uuid, file: PlatformFile, progress: ProgressListener? = null) {
        val fileBytes = file.readBytes()
        val response = httpClient.submitFormWithBinaryData(
            "inventory/lendings/$lendingId/add_memory",
            formData {
                append(
                    key = "file",
                    value = fileBytes,
                    headers = headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    }
                )
            }
        ) {
            progress?.let { onUpload(it) }
        }
        if (!response.status.isSuccess()) {
            throw ServerException.fromResponse(response)
        }
        val updatedLending = get(lendingId) ?: throw NoSuchElementException("Lending $lendingId not found after memory submission")
        LendingsRepository.update(updatedLending)
    }
}
