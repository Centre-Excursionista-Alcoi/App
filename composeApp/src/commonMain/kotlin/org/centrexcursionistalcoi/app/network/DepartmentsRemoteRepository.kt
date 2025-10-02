package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository

object DepartmentsRemoteRepository: RemoteRepository<Long, Department>(
    "/departments",
    Department.serializer(),
    DepartmentsRepository
)
