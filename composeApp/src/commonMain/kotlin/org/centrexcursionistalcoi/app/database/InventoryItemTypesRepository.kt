package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.database.data.InventoryItemTypes
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.storage.databaseInstance

object InventoryItemTypesRepository : DatabaseRepository<ReferencedInventoryItemType, Uuid>() {
    override val queries by lazy { databaseInstance.inventoryItemTypesQueries }

    override suspend fun get(id: Uuid): ReferencedInventoryItemType? {
        val departments = DepartmentsRepository.selectAll()
        return queries.get(id).awaitAsList().firstOrNull()?.toInventoryItemType(departments)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedInventoryItemType?> {
        val departments = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val itemType = queries.get(id).asFlow().mapToList(dispatcher)
        return combine(departments, itemType) { departmentsList, itemTypeList ->
            itemTypeList.firstOrNull()?.toInventoryItemType(departmentsList)
        }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedInventoryItemType>> {
        val departments = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val itemTypes = queries.selectAll().asFlow().mapToList(dispatcher)
        return combine(departments, itemTypes) { departmentsList, itemTypesList ->
            itemTypesList.map { it.toInventoryItemType(departmentsList) }
        }
    }

    override suspend fun selectAll(): List<ReferencedInventoryItemType> {
        val departments = DepartmentsRepository.selectAll()
        return queries.selectAll().awaitAsList().map { it.toInventoryItemType(departments) }
    }

    fun categoriesAsFlow(dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<Set<String>> = queries
        .categories()
        .asFlow()
        .mapToList(dispatcher)
        .map { it.flatten().toSet() }

    override suspend fun insert(item: ReferencedInventoryItemType) = queries.insert(
        id = item.id,
        displayName = item.displayName,
        description = item.description,
        categories = item.categories,
        department = item.department?.id,
        image = item.image
    )

    override suspend fun update(item: ReferencedInventoryItemType) = queries.update(
        id = item.id,
        displayName = item.displayName,
        description = item.description,
        categories = item.categories,
        department = item.department?.id,
        image = item.image
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun InventoryItemTypes.toInventoryItemType(departments: List<Department>) = InventoryItemType(
        id = id,
        displayName = displayName,
        description = description,
        categories = categories,
        department = department,
        image = image
    ).referenced(departments = departments)
}
