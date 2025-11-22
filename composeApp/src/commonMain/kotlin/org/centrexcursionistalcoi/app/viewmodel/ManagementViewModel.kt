package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
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
            DepartmentsRemoteRepository.update(
                departmentId,
                UpdateDepartmentRequest(
                    displayName = displayName,
                    image = image?.fileWithContext(),
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

    fun createInventoryItemType(displayName: String, description: String, categories: List<String>, department: Department?, imageFile: PlatformFile?) = launch {
        doAsync {
            InventoryItemTypesRemoteRepository.create(displayName, description.takeUnless { it.isEmpty() }, categories.takeUnless { it.isEmpty() }, department, imageFile)
        }
    }

    fun updateInventoryItemType(id: Uuid, displayName: String, description: String, categories: List<String>, department: Department?, imageFile: PlatformFile?) = launch {
        doAsync {
            InventoryItemTypesRemoteRepository.update(id, displayName, description.takeUnless { it.isEmpty() }, categories.takeUnless { it.isEmpty() }, department, imageFile)
        }
    }

    fun delete(inventoryItemType: ReferencedInventoryItemType) = launch {
        doAsync {
            InventoryItemTypesRemoteRepository.delete(inventoryItemType.id)
        }
    }

    fun createInventoryItem(variation: String, type: ReferencedInventoryItemType, amount: Int) = launch {
        doAsync {
            InventoryItemsRemoteRepository.create(variation, type.id, amount)
        }
    }

    fun delete(inventoryItem: ReferencedInventoryItem) = launch {
        doAsync {
            InventoryItemsRemoteRepository.delete(inventoryItem.id)
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

    fun createPost(
        title: String,
        department: Department?,
        content: RichTextState,
        link: String,
        files: List<PlatformFile>,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val contentMarkdown = content.toMarkdown()

            PostsRemoteRepository.create(
                title,
                contentMarkdown,
                department?.id,
                link.takeUnless { it.isBlank() },
                files,
                progressNotifier
            )
        }
    }

    fun updatePost(
        postId: Uuid,
        title: String?,
        department: Department?,
        content: RichTextState?,
        link: String?,
        removedFiles: List<Uuid>,
        files: List<PlatformFile>,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val contentMarkdown = content?.toMarkdown()

            PostsRemoteRepository.update(postId, title, contentMarkdown, department?.id, link, files, removedFiles, progressNotifier)
        }
    }

    fun delete(post: ReferencedPost) = launch {
        doAsync {
            PostsRemoteRepository.delete(post.id)
        }
    }
}
