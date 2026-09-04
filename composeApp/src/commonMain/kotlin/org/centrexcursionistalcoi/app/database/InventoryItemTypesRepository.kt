package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class InventoryItemTypesRepository(private val db: AppDatabase) : Repository<ReferencedInventoryItemType, Uuid> {
    private val log = logging()

    private val dao = db.inventoryItemTypeDao()

    override suspend fun get(id: Uuid): ReferencedInventoryItemType? = dao.get(id)?.toReferenced()

    override suspend fun getByIdList(ids: List<Uuid>): List<ReferencedInventoryItemType> = dao.getByIdList(ids).map { it.toReferenced() }

    override fun getAsFlow(id: Uuid): Flow<ReferencedInventoryItemType?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedInventoryItemType>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedInventoryItemType> = dao.selectAll().map { it.toReferenced() }

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
