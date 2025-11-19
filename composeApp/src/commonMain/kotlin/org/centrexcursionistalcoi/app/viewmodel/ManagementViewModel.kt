package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest

class ManagementViewModel : ViewModel() {
    fun createDepartment(displayName: String, imageFile: PlatformFile?, progressNotifier: ProgressNotifier?) = launch {
        try {
            doAsync {
                val image = imageFile?.readBytes()
                DepartmentsRemoteRepository.create(displayName, image, progressNotifier)
            }
        } catch (e: ServerException) {
            Napier.e(e) { "Could not create department." }
        } catch (e: Exception) {
            Napier.e(e) { "Could not create department due to an unexpected error." }
        }
    }

    fun updateDepartment(
        departmentId: Uuid,
        displayName: String,
        image: PlatformFile?,
        progressNotifier: ProgressNotifier? = null,
    ) = launch {
        doAsync {
            val imageBytes = image?.readBytes()
            DepartmentsRemoteRepository.update(
                departmentId,
                UpdateDepartmentRequest(
                    displayName = displayName,
                    image = imageBytes,
                ),
                UpdateDepartmentRequest.serializer(),
                progressNotifier,
            )
        }
    }

    fun delete(department: Department) = launch {
        doAsync {
            DepartmentsRemoteRepository.delete(department.id)
        }
    }

    fun createInventoryItemType(displayName: String, description: String, categories: List<String>, imageFile: PlatformFile?) = launch {
        doAsync {
            val image = imageFile?.readBytes()
            InventoryItemTypesRemoteRepository.create(displayName, description.takeUnless { it.isEmpty() }, categories.takeUnless { it.isEmpty() }, image)
        }
    }

    fun updateInventoryItemType(id: Uuid, displayName: String, description: String, categories: List<String>, imageFile: PlatformFile?) = launch {
        // TODO
    }

    fun delete(inventoryItemType: InventoryItemType) = launch {
        doAsync {
            InventoryItemTypesRepository.delete(inventoryItemType.id)
        }
    }

    fun delete(inventoryItem: ReferencedInventoryItem) = launch {
        doAsync {
            InventoryItemTypesRemoteRepository.delete(inventoryItem.id)
        }
    }

    fun promote(user: UserData) = launch {
        doAsync {
            UsersRemoteRepository.promote(user.sub)
            UsersRemoteRepository.update(user.sub)
        }
    }

    fun confirmLending(lending: ReferencedLending) = launch {
        doAsync {
            LendingsRemoteRepository.confirm(lending.id)
        }
    }

    fun skipLendingMemory(lending: ReferencedLending) = launch {
        doAsync {
            LendingsRemoteRepository.skipMemory(lending.id)
        }
    }
}
