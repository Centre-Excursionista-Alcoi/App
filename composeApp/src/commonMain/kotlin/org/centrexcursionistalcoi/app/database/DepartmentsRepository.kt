package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.data.Departments
import org.centrexcursionistalcoi.app.storage.databaseInstance

object DepartmentsRepository: Repository<Department, Long> {
    private val queries = databaseInstance.departmentsQueries

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
    )
}
