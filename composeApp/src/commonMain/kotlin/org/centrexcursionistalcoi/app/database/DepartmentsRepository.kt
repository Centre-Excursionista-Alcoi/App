package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.data.Departments
import org.centrexcursionistalcoi.app.storage.databaseInstance

object DepartmentsRepository : DatabaseRepository<Department, Uuid>() {
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

    override suspend fun get(id: Uuid): Department? {
        return queries.get(id).awaitAsList().firstOrNull()?.toDepartment()
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<Department?> {
        return queries
            .get(id)
            .asFlow()
            .mapToList(dispatcher)
            .map { departments ->
                departments.firstOrNull()?.toDepartment()
            }
    }

    override suspend fun insert(item: Department) = queries.insert(
        id = item.id,
        displayName = item.displayName,
        imageFile = item.image,
        members = item.members,
    )

    override suspend fun update(item: Department) = queries.update(
        id = item.id,
        displayName = item.displayName,
        imageFile = item.image,
        members = item.members,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun Departments.toDepartment() = Department(
        id = id,
        displayName = displayName,
        image = imageFile,
        members = members.orEmpty(),
    )
}
