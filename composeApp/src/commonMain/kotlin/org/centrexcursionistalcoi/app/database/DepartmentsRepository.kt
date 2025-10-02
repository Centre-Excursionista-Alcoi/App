package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.data.Department as DbDepartment
import org.centrexcursionistalcoi.app.storage.databaseInstance

object DepartmentsRepository {
    private val queries = databaseInstance.departmentQueries

    fun selectAllAsFlow(dispatcher: CoroutineDispatcher = Dispatchers.Default) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        // Convert from DbDepartment to Department
        .map { departments ->
            departments.map { it.toDepartment() }
        }

    fun selectAll() = queries.selectAll().executeAsList()

    suspend fun insert(department: Department) = queries.insert(
        id = department.id,
        displayName = department.displayName,
        imageFile = department.imageFile
    )

    suspend fun update(department: Department) = queries.update(
        id = department.id,
        displayName = department.displayName,
        imageFile = department.imageFile
    )

    suspend fun deleteByIdList(ids: List<Long>) {
        for (id in ids) {
            queries.deleteById(id)
        }
    }

    private fun DbDepartment.toDepartment() = Department(
        id = id,
        displayName = displayName,
        imageFile = imageFile
    )
}
