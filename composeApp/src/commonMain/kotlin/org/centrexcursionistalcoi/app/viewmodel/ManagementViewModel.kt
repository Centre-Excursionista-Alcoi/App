package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.*
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import kotlin.uuid.Uuid

class ManagementViewModel : ViewModel() {
    companion object {
        private val log = logging()
    }
    
    fun createDepartment(displayName: String, imageFile: PlatformFile?, progressNotifier: ProgressNotifier?) = launch {
        try {
            doAsync {
                val image = imageFile?.readBytes()
                DepartmentsRemoteRepository.create(displayName, image, progressNotifier)
            }
        } catch (e: ServerException) {
            log.e(e) { "Could not create department." }
        } catch (e: Exception) {
            log.e(e) { "Could not create department due to an unexpected error." }
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

    fun kickFromDepartment(userData: UserData, department: Department) = launch {
        doAsync {
            DepartmentsRemoteRepository.kick(department.id, userData.sub)
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
            UsersRemoteRepository.update(user.sub, ignoreIfModifiedSince = true)
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

    fun createEvent(
        start: LocalDateTime,
        end: LocalDateTime?,
        place: String,
        title: String,
        description: RichTextState,
        maxPeople: String,
        requiresConfirmation: Boolean,
        department: Department?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val descriptionMarkdown = description.toMarkdown()

            EventsRemoteRepository.create(
                start,
                end,
                place,
                title,
                descriptionMarkdown,
                maxPeople,
                requiresConfirmation,
                department?.id,
                image,
                progressNotifier
            )
        }
    }

    fun updateEvent(
        eventId: Uuid,
        start: LocalDateTime?,
        end: LocalDateTime?,
        place: String?,
        title: String?,
        description: RichTextState?,
        maxPeople: String?,
        requiresConfirmation: Boolean?,
        department: Department?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val descriptionMarkdown = description?.toMarkdown()

            EventsRemoteRepository.update(
                eventId,
                start,
                end,
                place,
                title,
                descriptionMarkdown,
                maxPeople,
                requiresConfirmation,
                department?.id,
                image,
                progressNotifier
            )
        }
    }

    fun delete(post: ReferencedEvent) = launch {
        doAsync {
            EventsRemoteRepository.delete(post.id)
        }
    }
}
