package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem

object DepartmentsRemoteRepository : RemoteRepository<Long, Department>(
    "/departments",
    Department.serializer(),
    DepartmentsRepository
) {
    suspend fun create(name: String, image: ByteArray?) {
        val imageUuid = image?.let {
            val uuid = Uuid.random()
            PlatformFileSystem.write("temp/$uuid", it)
            uuid
        }

        create(Department(0L, name, imageUuid))
    }
}
