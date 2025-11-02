package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository

class InventoryItemModel(private val typeId: Uuid): ViewModel() {
    val type = InventoryItemTypesRepository.getAsFlow(typeId).stateInViewModel()
    val categories = InventoryItemTypesRepository.categoriesAsFlow().stateInViewModel()
    val items = InventoryItemsRepository.selectAllWithTypeIdFlow(typeId).stateInViewModel()

    /**
     * Deletes the inventory item type.
     */
    fun delete() = launch {
        doAsync { InventoryItemTypesRemoteRepository.delete(typeId) }
    }

    /**
     * Deletes the given inventory item.
     */
    fun delete(item: ReferencedInventoryItem) = launch {
        doAsync { InventoryItemsRemoteRepository.delete(item.id) }
    }

    fun updateInventoryItem(item: ReferencedInventoryItem, variation: String) = launch {
        doAsync {
            InventoryItemsRemoteRepository.update(
                item.id,
                variation.takeUnless { it.isEmpty() }
            )
        }
    }

    fun createInventoryItem(variation: String, type: InventoryItemType, amount: Int) = launch {
        doAsync { InventoryItemsRemoteRepository.create(variation.takeUnless { it.isEmpty() }, type.id, amount) }
    }

    fun updateInventoryItemType(id: Uuid, displayName: String?, description: String?, category: String?, imageFile: PlatformFile?) = launch {
        doAsync {
            val image = imageFile?.readBytes()
            InventoryItemTypesRemoteRepository.update(id, displayName, description, category, image)
        }
    }
}
