package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity.Companion.toEntity
import kotlin.uuid.Uuid

class DepartmentsRepository(db: AppDatabase) : Repository<Department, Uuid> {
    private val dao = db.departmentDao()

    override fun selectAllAsFlow() = dao
        .selectAllAsFlow()
        .map { list ->
            list.map { it.toDepartment() }
        }

    override suspend fun selectAll() = dao.selectAll().map { it.toDepartment() }

    override suspend fun get(id: Uuid): Department? {
        return dao.get(id)?.toDepartment()
    }

    override fun getAsFlow(id: Uuid): Flow<Department?> {
        return dao
            .getAsFlow(id)
            .map { entity ->
                entity?.toDepartment()
            }
    }

    override suspend fun insert(item: Department) = dao.insert(
        item.toEntity()
    )

    override suspend fun update(item: Department) = dao.update(
        item.toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
