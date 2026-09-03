package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class EventsManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    eventsRepository: EventsRepository,
    departmentsRepository: DepartmentsRepository,
    private val eventsRemoteRepository: EventsRemoteRepository
) : ViewModel() {
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val events = eventsRepository.selectAllAsFlow().stateInViewModel()

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
        withContext(dispatcherProvider.io) {
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
        withContext(dispatcherProvider.io) {
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

    fun deleteEvent(post: ReferencedEvent) = launch {
        withContext(dispatcherProvider.io) {
            eventsRemoteRepository.delete(post.id)
        }
    }
}
