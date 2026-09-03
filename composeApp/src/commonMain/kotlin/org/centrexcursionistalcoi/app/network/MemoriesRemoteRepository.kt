package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_MEMORIES_SYNC
import kotlin.uuid.Uuid

/**
 * Memories are their own resource on the server (`/memories`) and can, in general, exist without a lending
 * attached. This app does not expose creating a lending-less memory yet -- the only supported creation path is
 * [LendingsRemoteRepository.submitMemory], which always attaches a lending -- so entity creation through this
 * repository is disabled.
 *
 * Reading, updating and deleting are kept available (and synced) so the local database and repository layer are
 * ready to accept lending-less memories once the app exposes creating them.
 */
class MemoriesRemoteRepository(
    memoriesRepository: MemoriesRepository,
    usersRepository: UsersRepository,
    membersRepository: MembersRepository,
    departmentsRepository: DepartmentsRepository,
) : RemoteRepository<Uuid, ReferencedMemory, Uuid, Memory>(
    "/memories",
    SETTINGS_LAST_MEMORIES_SYNC,
    Memory.serializer(),
    memoriesRepository,
    isCreationSupported = false,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { memory ->
        memory.referenced(usersRepository.selectAll(), membersRepository.selectAll(), departmentsRepository.selectAll())
    },
) {
    suspend fun create(
        place: String?,
        members: List<Member>,
        externalUsers: String?,
        text: String,
        sport: Sports?,
        department: Department?,
        attachments: List<PlatformFile>,
        from: ZonedDateTime,
        to: ZonedDateTime,
        progress: ProgressNotifier? = null,
    ) {
        val filesWithContext = attachments.map { it.fileWithContext() }

        val response = httpClient.submitFormWithBinaryData(
            "memories",
            formData {
                append("from", from.toString())
                append("to", to.toString())

                place?.takeIf { it.isNotBlank() }?.let { append("place", it) }
                append("members", members.joinToString(",") { it.memberNumber.toString() })
                externalUsers?.takeIf { it.isNotBlank() }?.let { append("external_users", it) }
                sport?.let { append("sport", it.name) }
                department?.let { append("department", it.id.toString()) }
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
        update(memoryId, progress)
    }

    /**
     * Patches the memory with the given [id]. Named `patch` (rather than `update`) to avoid ambiguity with the
     * inherited no-op-body [update] overload, since every parameter here is optional too.
     */
    suspend fun patch(
        id: Uuid,
        place: String? = null,
        members: List<UInt>? = null,
        externalUsers: String? = null,
        text: String? = null,
        sport: Sports? = null,
        department: Uuid? = null,
        attachments: List<FileWithContext>? = null,
        from: ZonedDateTime? = null,
        to: ZonedDateTime? = null,
        progressNotifier: ProgressNotifier? = null,
    ) = update(
        id,
        UpdateMemoryRequest(place, members, externalUsers, text, sport, department, attachments, from, to),
        UpdateMemoryRequest.serializer(),
        progressNotifier,
    )
}
