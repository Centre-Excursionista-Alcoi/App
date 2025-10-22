package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator

object DepartmentsRemoteRepository : SymmetricRemoteRepository<Int, Department>(
    "/departments",
    Department.serializer(),
    DepartmentsRepository
) {
    suspend fun create(name: String, image: ByteArray?) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        create(Department(0, name, imageUuid))
    }
}
