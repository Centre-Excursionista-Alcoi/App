package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.data.Departments
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val DepartmentsRepository : Repository<Department, Int>

object DepartmentsSettingsRepository : SettingsRepository<Department, Int>("departments", Department.serializer())

object DepartmentsDatabaseRepository : DatabaseRepository<Department, Int>() {
    override val queries by lazy { databaseInstance.departmentsQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        // Convert from DbDepartment to Department
        .map { departments ->
            departments.map { it.toDepartment() }
        }

    override suspend fun selectAll() = queries.selectAll().awaitAsList().map { it.toDepartment() }

    override suspend fun get(id: Int): Department? {
        return queries.get(id.toLong()).awaitAsList().firstOrNull()?.toDepartment()
    }

    override suspend fun insert(item: Department) = queries.insert(
        id = item.id.toLong(),
        displayName = item.displayName,
        imageFile = item.image
    )

    override suspend fun update(item: Department) = queries.update(
        id = item.id.toLong(),
        displayName = item.displayName,
        imageFile = item.image
    )

    override suspend fun delete(id: Int) {
        queries.deleteById(id.toLong())
    }

    private fun Departments.toDepartment() = Department(
        id = id.toInt(),
        displayName = displayName,
        image = imageFile
    )
}
