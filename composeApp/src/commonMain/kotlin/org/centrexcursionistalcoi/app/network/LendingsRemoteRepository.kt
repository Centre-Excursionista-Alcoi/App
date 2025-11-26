package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedLending.Companion.referenced
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.exception.NoValidInsuranceForPeriodException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.ReturnLendingRequest
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_LENDINGS_SYNC

object LendingsRemoteRepository : RemoteRepository<Uuid, ReferencedLending, Uuid, Lending>(
    "/inventory/lendings",
    SETTINGS_LAST_LENDINGS_SYNC,
    Lending.serializer(),
    LendingsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { lending ->
        val inventoryItemTypes = InventoryItemTypesRepository.selectAll()
        val users = UsersRepository.selectAll()
        lending.referenced(users, inventoryItemTypes)
    },
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
            throw response.bodyAsError().toThrowable()
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
     * @throws NoValidInsuranceForPeriodException if the user does not have valid insurance for the specified period.
     * @throws ServerException for other allocation failures.
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
            val error = response.bodyAsError()
            when (error.code) {
                Error.ERROR_LENDING_CONFLICT -> {
                    val availableItemIds = response.headers["CEA-Available-Items"]
                        ?.split(',')
                        ?.filter { it.isNotEmpty() }
                        ?.map { Uuid.parse(it) }
                    throw CannotAllocateEnoughItemsException(typeId, availableItemIds, amount)
                }
                Error.ERROR_USER_DOES_NOT_HAVE_INSURANCE -> {
                    throw NoValidInsuranceForPeriodException()
                }

                else -> {
                    throw response.bodyAsError().toThrowable()
                }
            }
        }
    }

    /**
     * Cancels a lending request by its ID.
     * The logged-in user must be the owner of the lending.
     * The lending must not have been picked up yet.
     * @param lendingId The UUID of the lending to cancel.
     * @throws ServerException if the cancellation fails.
     */
    suspend fun cancel(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post("inventory/lendings/$lendingId/cancel") {
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsError()
            throw error.toThrowable()
        }
        LendingsRepository.delete(lendingId)
    }

    /**
     * Confirms a lending request by its ID.
     * The logged-in user must have the necessary permissions to confirm lendings.
     * @param lendingId The UUID of the lending to confirm.
     * @throws ServerException if the confirmation fails.
     * @throws NoSuchElementException if the lending is not found after confirmation.
     */
    suspend fun confirm(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post("inventory/lendings/$lendingId/confirm") {
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after confirmation")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Marks a lending as picked up by its ID.
     * The logged-in user must have the necessary permissions to pickup lendings.
     * @param lendingId The UUID of the lending to pickup.
     * @param dismissItemsIds The list of item UUIDs to dismiss from the lending. Can be empty.
     * @throws ServerException if the pickup fails.
     * @throws NoSuchElementException if the lending is not found after pickup.
     */
    suspend fun pickup(
        lendingId: Uuid,
        dismissItemsIds: List<Uuid>,
        progress: ProgressNotifier? = null
    ) {
        val response = httpClient.submitForm(
            "inventory/lendings/$lendingId/pickup",
            formParameters = parameters {
                if (dismissItemsIds.isNotEmpty()) {
                    append("dismiss_items", dismissItemsIds.joinToString(","))
                }
            },
        ) {
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after pickup")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Marks a lending as returned by its ID.
     * The logged-in user must have the necessary permissions to receive lendings.
     * @param lendingId The UUID of the lending to receive.
     * @param items A list of pairs containing the item UUIDs and optional notes.
     * @param progress An optional progress listener for upload progress.
     * @throws ServerException if the reception fails.
     * @throws NoSuchElementException if the lending is not found after receive.
     */
    suspend fun `return`(
        lendingId: Uuid,
        items: List<Pair<Uuid, String?>>,
        progress: ProgressNotifier? = null
    ) {
        val response = httpClient.post("inventory/lendings/$lendingId/return") {
            monitorUploadProgress(progress)
            contentType(ContentType.Application.Json)
            setBody(
                ReturnLendingRequest(
                    items.map { (itemId, notes) ->
                        ReturnLendingRequest.ReturnedItem(itemId, notes)
                    }
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after return")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Submits a memory file for a lending by its ID.
     * The logged-in user must be the owner of the lending.
     * @param lendingId The UUID of the lending to submit the memory for.
     * @param place The place where the activity took place.
     * @param memberUsers The list of member users who participated in the activity.
     * @param externalUsers A string describing external users who participated in the activity.
     * @param text The rich text content of the memory in Markdown.
     * @param files The list of memory files to submit.
     * @param progress An optional progress listener for upload progress.
     * @throws ServerException if the submission fails.
     * @throws NoSuchElementException if the lending is not found after submission.
     */
    suspend fun submitMemory(
        lendingId: Uuid,
        place: String,
        memberUsers: List<UserData>,
        externalUsers: String,
        sport: Sports?,
        text: String,
        files: List<PlatformFile>,
        progress: ProgressNotifier? = null
    ) {
        val filesWithContext = files.map { it.fileWithContext() }

        val response = httpClient.submitFormWithBinaryData(
            "inventory/lendings/$lendingId/add_memory",
            formData {
                place.takeIf { it.isNotBlank() }?.let { append("place", it) }
                append("users", memberUsers.joinToString(",") { it.sub })
                externalUsers.takeIf { it.isNotBlank() }?.let { append("external_users", it) }
                sport?.let { append("sport", it.name) }
                append("text",  text)

                filesWithContext.mapIndexed { index, file ->
                    append(
                        key = "file_$index",
                        value = file.bytes,
                        headers = headers {
                            append(HttpHeaders.ContentType, (file.contentType ?: ContentType.Application.OctetStream).toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name ?: "file_$index"}\"")
                        }
                    )
                }
            }
        ) {
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after memory submission")
        LendingsRepository.update(updatedLending)
    }

    /**
     * Skips the memory submission for a lending by its ID.
     * @param lendingId The UUID of the lending to skip the memory for.
     * @throws ServerException if the skip memory operation fails.
     * @throws NoSuchElementException if the lending is not found after skipping memory.
     */
    suspend fun skipMemory(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post("inventory/lendings/$lendingId/skip_memory") {
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after skipping memory")
        LendingsRepository.update(updatedLending)
    }
}
