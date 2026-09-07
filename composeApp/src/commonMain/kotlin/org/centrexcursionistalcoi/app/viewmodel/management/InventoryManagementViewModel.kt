package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class InventoryManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    inventoryItemsRepository: InventoryItemsRepository,
    inventoryItemTypesRepository: InventoryItemTypesRepository,
    private val inventoryItemsRemoteRepository: InventoryItemsRemoteRepository,
    private val inventoryItemTypesRemoteRepository: InventoryItemTypesRemoteRepository
) : ViewModel() {
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItems = inventoryItemsRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypes = inventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()
    val inventoryItemTypesCategories = inventoryItemTypesRepository.categoriesAsFlow().stateInViewModel()

    fun createInventoryItem(variation: String, type: ReferencedInventoryItemType, amount: Int) =
        launch {
            withContext(dispatcherProvider.io) {
                inventoryItemsRemoteRepository.create(variation, type.id, amount)
            }
        }

    fun delete(inventoryItem: ReferencedInventoryItem) = launch {
        withContext(dispatcherProvider.io) {
            inventoryItemsRemoteRepository.delete(inventoryItem.id)
        }
    }

    fun createInventoryItemType(
        displayName: String,
        description: String,
        categories: List<String>,
        weight: String,
        department: Department?,
        imageFile: PlatformFile?
    ) = launch {
        withContext(dispatcherProvider.io) {
            val weightDouble = weight.toDoubleOrNull()

            inventoryItemTypesRemoteRepository.create(
                displayName,
                description.takeUnless { it.isEmpty() },
                categories.takeUnless { it.isEmpty() },
                weightDouble?.takeIf { it > 0.0 },
                department,
                imageFile
            )
        }
    }

    fun updateInventoryItemType(
        id: Uuid,
        displayName: String,
        description: String,
        categories: List<String>,
        weight: String,
        department: Department?,
        imageFile: PlatformFile?
    ) = launch {
        withContext(dispatcherProvider.io) {
            val weightDouble = weight.toDoubleOrNull()

            inventoryItemTypesRemoteRepository.update(
                id,
                displayName,
                description.takeUnless { it.isEmpty() },
                categories.takeUnless { it.isEmpty() },
                weightDouble?.takeIf { it > 0.0 },
                department,
                imageFile
            )
        }
    }

    fun delete(inventoryItemType: ReferencedInventoryItemType) = launch {
        withContext(dispatcherProvider.io) {
            inventoryItemTypesRemoteRepository.delete(inventoryItemType.id)
        }
    }

    fun updateInventoryItemManufacturerData(item: ReferencedInventoryItem, data: String) = launch {
        withContext(dispatcherProvider.io) {
            inventoryItemsRemoteRepository.updateManufacturerData(item.id, data)
        }
    }
}
