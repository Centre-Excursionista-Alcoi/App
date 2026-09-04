package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class InventoryItemsRepository(db: AppDatabase) : Repository<ReferencedInventoryItem, Uuid> {
    private val dao = db.inventoryItemDao()

    override fun selectAllAsFlow(): Flow<List<ReferencedInventoryItem>> = dao
        .selectAllAsFlow()
        .map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedInventoryItem> = dao.selectAll().map { it.toReferenced() }

    fun selectAllWithTypeIdFlow(typeId: Uuid): Flow<List<ReferencedInventoryItem>> = dao
        .selectAllByTypeAsFlow(typeId)
        .map { list -> list.map { it.toReferenced() } }

    override suspend fun get(id: Uuid): ReferencedInventoryItem? = dao.get(id)?.toReferenced()

    override fun getAsFlow(id: Uuid): Flow<ReferencedInventoryItem?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override suspend fun insert(item: ReferencedInventoryItem) = dao.insert(
        item.dereference().toEntity()
    )

    override suspend fun update(item: ReferencedInventoryItem) = dao.update(
        item.dereference().toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
