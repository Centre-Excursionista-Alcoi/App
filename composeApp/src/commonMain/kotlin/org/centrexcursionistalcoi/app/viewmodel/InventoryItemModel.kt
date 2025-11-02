package org.centrexcursionistalcoi.app.viewmodel

import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.jetbrains.compose.resources.getString

class InventoryItemModel(private val typeId: Uuid): ErrorViewModel() {
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
        doAsync {
            try {
                InventoryItemsRemoteRepository.delete(item.id)
            } catch (e: ServerException) {
                if (e.errorCode == Error.ERROR_ENTITY_DELETE_REFERENCES_EXIST) {
                    // There are references to this item, so we cannot delete it
                    setError(getString(Res.string.error_delete_references))
                } else {
                    throw e
                }
            }
        }
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
