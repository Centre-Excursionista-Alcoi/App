package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class ManagementViewModel(
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val inventoryItemTypesRemoteRepository: InventoryItemTypesRemoteRepository,
    private val inventoryItemsRemoteRepository: InventoryItemsRemoteRepository,
    private val usersRemoteRepository: UsersRemoteRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val postsRemoteRepository: PostsRemoteRepository,
    private val eventsRemoteRepository: EventsRemoteRepository,
) : ViewModel() {
    companion object {
        private val log = logging()
    }

    fun createDepartment(
        displayName: String,
        imageFile: PlatformFile?,
        progressNotifier: ProgressNotifier?
    ) = launch {
        try {
            doAsync {
                val image = imageFile?.readBytes()
                departmentsRemoteRepository.create(displayName, image, progressNotifier)
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
            departmentsRemoteRepository.update(
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
            departmentsRemoteRepository.delete(department.id)
        }
    }

    fun kickFromDepartment(userData: UserData, department: Department) = launch {
        doAsync {
            departmentsRemoteRepository.kick(department.id, userData.sub)
        }
    }

    fun approveDepartmentJoinRequest(request: DepartmentMemberInfo) = launch {
        departmentsRemoteRepository.confirmJoinRequest(request)
    }

    fun denyDepartmentJoinRequest(request: DepartmentMemberInfo) = launch {
        departmentsRemoteRepository.denyJoinRequest(request)
    }

    fun createInventoryItemType(
        displayName: String,
        description: String,
        categories: List<String>,
        weight: String,
        department: Department?,
        imageFile: PlatformFile?
    ) = launch {
        doAsync {
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
        doAsync {
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
        doAsync {
            inventoryItemTypesRemoteRepository.delete(inventoryItemType.id)
        }
    }

    fun createInventoryItem(variation: String, type: ReferencedInventoryItemType, amount: Int) =
        launch {
            doAsync {
                inventoryItemsRemoteRepository.create(variation, type.id, amount)
            }
        }

    fun delete(inventoryItem: ReferencedInventoryItem) = launch {
        doAsync {
            inventoryItemsRemoteRepository.delete(inventoryItem.id)
        }
    }

    fun promote(user: UserData) = launch {
        doAsync {
            usersRemoteRepository.promote(user.sub)
            usersRemoteRepository.update(user.sub, ignoreIfModifiedSince = true)
        }
    }

    fun confirmLending(lending: ReferencedLending) = launch {
        doAsync {
            lendingsRemoteRepository.confirm(lending.id)
        }
    }

    fun skipLendingMemory(lending: ReferencedLending) = launch {
        doAsync {
            lendingsRemoteRepository.skipMemory(lending.id)
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

            postsRemoteRepository.create(
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

            postsRemoteRepository.update(
                postId,
                title,
                contentMarkdown,
                department?.id,
                link,
                files,
                removedFiles,
                progressNotifier
            )
        }
    }

    fun delete(post: ReferencedPost) = launch {
        doAsync {
            postsRemoteRepository.delete(post.id)
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
        requiresInsurance: Boolean,
        department: Department?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val descriptionMarkdown = description.toMarkdown()

            eventsRemoteRepository.create(
                start,
                end,
                place,
                title,
                descriptionMarkdown,
                maxPeople,
                requiresConfirmation,
                requiresInsurance,
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
        requiresInsurance: Boolean?,
        department: Department?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        doAsync {
            val descriptionMarkdown = description?.toMarkdown()

            eventsRemoteRepository.update(
                eventId,
                start,
                end,
                place,
                title,
                descriptionMarkdown,
                maxPeople,
                requiresConfirmation,
                requiresInsurance,
                department?.id,
                image,
                progressNotifier
            )
        }
    }

    fun delete(post: ReferencedEvent) = launch {
        doAsync {
            eventsRemoteRepository.delete(post.id)
        }
    }

    fun updateInventoryItemManufacturerData(item: ReferencedInventoryItem, data: String) = launch {
        doAsync {
            inventoryItemsRemoteRepository.updateManufacturerData(item.id, data)
        }
    }
}
