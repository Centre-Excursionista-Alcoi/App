package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.data.Department

object DepartmentsRepository : SettingsRepository<Department, Long>("departments", Department.serializer()) { // DatabaseRepository<Department, Long>()
    /*override val queries by lazy { databaseInstance.departmentsQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        // Convert from DbDepartment to Department
        .map { departments ->
            departments.map { it.toDepartment() }
        }

    override suspend fun selectAll() = queries.selectAll().awaitAsList().map { it.toDepartment() }

    override suspend fun insert(item: Department) = queries.insert(
        id = item.id,
        displayName = item.displayName,
        imageFile = item.imageFile
    )

    override suspend fun update(item: Department) = queries.update(
        id = item.id,
        displayName = item.displayName,
        imageFile = item.imageFile
    )

    override suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    private fun Departments.toDepartment() = Department(
        id = id,
        displayName = displayName,
        imageFile = imageFile
    )*/
}
