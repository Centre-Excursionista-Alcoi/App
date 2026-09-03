package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class InventoryItemTypesRepository(
    private val db: AppDatabase,
    private val departmentsRepository: DepartmentsRepository,
) : Repository<ReferencedInventoryItemType, Uuid> {
    private val log = logging()

    private val dao = db.inventoryItemTypeDao()

    override suspend fun get(id: Uuid): ReferencedInventoryItemType? {
        val departments = departmentsRepository.selectAll()
        return dao.get(id)?.toInventoryItemType()?.referenced(departments)
    }

    override fun getAsFlow(id: Uuid): Flow<ReferencedInventoryItemType?> {
        val departments = departmentsRepository.selectAllAsFlow()
        val itemType = dao.getAsFlow(id)
        return combine(departments, itemType) { departmentsList, itemTypeEntity ->
            itemTypeEntity?.toInventoryItemType()?.referenced(departmentsList)
        }
    }

    override fun selectAllAsFlow(): Flow<List<ReferencedInventoryItemType>> {
        val departments = departmentsRepository.selectAllAsFlow()
        val itemTypes = dao.selectAllAsFlow()
        return combine(departments, itemTypes) { departmentsList, itemTypesList ->
            itemTypesList.map { it.toInventoryItemType().referenced(departmentsList) }
        }
    }

    override suspend fun selectAll(): List<ReferencedInventoryItemType> {
        val departments = departmentsRepository.selectAll()
        return dao.selectAll().map { it.toInventoryItemType().referenced(departments) }
    }

    fun categoriesAsFlow(): Flow<Set<String>> = dao
        .selectAllWithCategoriesAsFlow()
        .map { list -> list.flatMap { it.categories.orEmpty() }.toSet() }

    override suspend fun insert(item: ReferencedInventoryItemType) = dao.insert(
        item.dereference().toEntity()
    )

    override suspend fun update(item: ReferencedInventoryItemType) = dao.update(
        item.dereference().toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }

    /**
     * Deletes all item types associated with the given department ID,
     * along with their corresponding inventory items.
     * @param departmentId The ID of the department whose item types are to be deleted.
     */
    suspend fun deleteByDepartmentId(departmentId: Uuid) {
        val types = dao.selectByDepartmentId(departmentId)
        log.d { "Got ${types.size} types for department $departmentId" }
        val inventoryItemDao = db.inventoryItemDao()
        for (type in types) {
            val items = inventoryItemDao.selectAllByType(type.id)
            log.d { "  Deleting ${items.size} items for type ${type.id}" }
            for (item in items) {
                inventoryItemDao.deleteById(item.id)
            }
            log.d { "  Deleting type ${type.id}" }
            dao.deleteById(type.id)
        }
    }
}
