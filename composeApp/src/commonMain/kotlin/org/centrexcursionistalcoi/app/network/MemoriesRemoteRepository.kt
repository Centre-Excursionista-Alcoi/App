package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository.update
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
object MemoriesRemoteRepository : RemoteRepository<Uuid, ReferencedMemory, Uuid, Memory>(
    "/memories",
    SETTINGS_LAST_MEMORIES_SYNC,
    Memory.serializer(),
    MemoriesRepository,
    isCreationSupported = false,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { memory ->
        memory.referenced(UsersRepository.selectAll(), MembersRepository.selectAll(), DepartmentsRepository.selectAll())
    },
) {
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
