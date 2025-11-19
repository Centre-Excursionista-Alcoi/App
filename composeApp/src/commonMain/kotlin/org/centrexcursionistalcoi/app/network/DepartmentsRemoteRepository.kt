package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.utils.Zero

object DepartmentsRemoteRepository : SymmetricRemoteRepository<Uuid, Department>(
    "/departments",
    Department.serializer(),
    DepartmentsRepository
) {
    suspend fun create(displayName: String, image: ByteArray?, progressNotifier: ProgressNotifier? = null) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        Napier.i { "Creating a new department: displayName=\"${displayName}\", imageUuid=${imageUuid}" }

        create(Department(Uuid.Zero, displayName, imageUuid), progressNotifier)
    }
}
