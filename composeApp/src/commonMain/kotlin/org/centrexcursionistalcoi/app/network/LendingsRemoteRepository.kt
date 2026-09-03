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
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.data.referenced
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.exception.NoValidInsuranceForPeriodException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.DeleteLendingRequest
import org.centrexcursionistalcoi.app.request.ReturnLendingRequest
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_LENDINGS_SYNC
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class LendingsRemoteRepository(
    private val lendingsRepository: LendingsRepository,
    inventoryItemTypesRepository: InventoryItemTypesRepository,
    usersRepository: UsersRepository,
    membersRepository: MembersRepository,
    departmentsRepository: DepartmentsRepository,
    memoriesRepository: MemoriesRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,
) : RemoteRepository<Uuid, ReferencedLending, Uuid, Lending>(
    "/inventory/lendings",
    SETTINGS_LAST_LENDINGS_SYNC,
    Lending.serializer(),
    lendingsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { lending ->
        val inventoryItemTypes = inventoryItemTypesRepository.selectAll()
        val users = usersRepository.selectAll()
        val members = membersRepository.selectAll()
        val departments = departmentsRepository.selectAll()
        // Memories are synced separately (see MemoriesRemoteRepository); this only resolves against what's
        // already cached locally, so Memories must be synced before Lendings for this to be up to date.
        val memory = lending.memory?.let { memoriesRepository.get(it) }
        lending.referenced(users, inventoryItemTypes, memory)
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
            lendingsRepository.insert(lending)
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun delete(id: Uuid, reason: String?, progressNotifier: ProgressNotifier? = null) {
        delete(id, DeleteLendingRequest(reason), DeleteLendingRequest.serializer(), progressNotifier)
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
        lendingsRepository.delete(lendingId)
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
        lendingsRepository.update(updatedLending)
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
        lendingsRepository.update(updatedLending)
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
        lendingsRepository.update(updatedLending)
    }

    /**
     * Submits a memory for a lending by its ID.
     *
     * Memories are their own resource on the server (`POST /memories`) and can, in general, exist without a
     * lending attached — but this app only ever submits memories tied to a lending, so [lendingId] is required here.
     *
     * The logged-in user must be the owner of the lending.
     * @param lendingId The UUID of the lending to submit the memory for.
     * @param place The place where the activity took place.
     * @param members The list of members who participated in the activity.
     * @param externalUsers A string describing external users who participated in the activity.
     * @param sport The sport associated with the activity.
     * @param department The department associated with the activity.
     * @param text The rich text content of the memory in Markdown.
     * @param files The list of memory files to submit.
     * @param progress An optional progress listener for upload progress.
     * @throws ServerException if the submission fails.
     * @throws NoSuchElementException if the lending is not found after submission.
     */
    suspend fun submitMemory(
        lendingId: Uuid,
        place: String,
        members: List<Member>,
        externalUsers: String,
        sport: Sports?,
        department: Department?,
        text: String,
        files: List<PlatformFile>,
        progress: ProgressNotifier? = null
    ) {
        val filesWithContext = files.map { it.fileWithContext() }

        val response = httpClient.submitFormWithBinaryData(
            "memories",
            formData {
                place.takeIf { it.isNotBlank() }?.let { append("place", it) }
                append("members", members.joinToString(",") { it.memberNumber.toString() })
                externalUsers.takeIf { it.isNotBlank() }?.let { append("external_users", it) }
                sport?.let { append("sport", it.name) }
                department?.let { append("department", it.id.toString()) }
                append("lending", lendingId.toString())
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

        // Fetch and cache the newly created memory locally, so that resolving the lending's memory (by id) below
        // finds it. The created memory's id is only available via the Location header of this response.
        val location = response.headers[HttpHeaders.Location]
            ?: throw IllegalArgumentException("Missing Location header in response")
        val memoryId = location.substringAfterLast('/').let { Uuid.parse(it) }
        memoriesRemoteRepository.update(memoryId, progress)

        val updatedLending = get(lendingId, progress) ?: throw NoSuchElementException("Lending $lendingId not found after memory submission")
        lendingsRepository.update(updatedLending)
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
        lendingsRepository.update(updatedLending)
    }
}
