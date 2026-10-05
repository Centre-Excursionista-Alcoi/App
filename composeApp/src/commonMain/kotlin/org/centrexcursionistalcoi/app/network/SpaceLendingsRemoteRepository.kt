package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.SpaceOccupancy
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.AttachPaymentProofRequest
import org.centrexcursionistalcoi.app.request.CreateSpaceLendingRequest
import org.centrexcursionistalcoi.app.request.SetSpaceLendingPaymentRequest
import org.centrexcursionistalcoi.app.request.SubmitSpaceLendingReportRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceLendingAttendeesRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceLendingRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_SPACE_LENDINGS_SYNC
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

/**
 * Space lendings are a 3-step pipeline: the user creates the lending (and can modify it), a manager hands the keys
 * over (`pickup`), and takes them back (`returnKeys`), after which it is paid.
 */
@Singleton
class SpaceLendingsRemoteRepository(
    private val spaceLendingsRepository: SpaceLendingsRepository,
) : RemoteRepository<Uuid, SpaceLending, Uuid, SpaceLending>(
    Api.SpaceLendings.resources,
    SETTINGS_LAST_SPACE_LENDINGS_SYNC,
    SpaceLending.serializer(),
    spaceLendingsRepository,
    isPatchSupported = false,
    remoteToLocalIdConverter = { it },
) {
    private fun id(lendingId: Uuid) = Api.SpaceLendings.Id(lendingId.toString())

    /** Fetches the lending again, after an operation changed it. */
    private suspend fun refresh(lendingId: Uuid, progress: ProgressNotifier?) {
        update(lendingId, progress, ignoreIfModifiedSince = true)
            ?: throw NoSuchElementException("Space lending $lendingId not found after the operation")
    }

    /**
     * The nights of a space that are taken, from today on. Doesn't tell who by.
     */
    suspend fun occupancy(spaceId: Uuid): List<SpaceOccupancy> {
        val response = httpClient.get(Api.Spaces.Id.Occupancy(Api.Spaces.Id(spaceId.toString())))
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        return json.decodeFromString(ListSerializer(SpaceOccupancy.serializer()), response.bodyAsText())
    }

    /**
     * Creates a lending.
     * @return The id of the new lending.
     */
    suspend fun create(request: CreateSpaceLendingRequest, progress: ProgressNotifier? = null): Uuid {
        val response = httpClient.post(Api.SpaceLendings()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateSpaceLendingRequest.serializer(), request))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        val location = response.headers[HttpHeaders.Location]
            ?: throw IllegalArgumentException("Missing Location header in response")
        val lendingId = Uuid.parse(location.substringAfterLast('/'))
        refresh(lendingId, progress)
        return lendingId
    }

    /** Changes a lending that hasn't been picked up yet. */
    suspend fun modify(lendingId: Uuid, request: UpdateSpaceLendingRequest, progress: ProgressNotifier? = null) {
        val response = httpClient.patch(id(lendingId)) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateSpaceLendingRequest.serializer(), request))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    suspend fun cancel(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post(Api.SpaceLendings.Id.Cancel(id(lendingId))) { monitorUploadProgress(progress) }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /** Changes the people that attended. Until the lending is paid. */
    suspend fun updateAttendees(lendingId: Uuid, attendees: Map<Category, Int>, progress: ProgressNotifier? = null) {
        val response = httpClient.post(Api.SpaceLendings.Id.Attendees(id(lendingId))) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateSpaceLendingAttendeesRequest.serializer(), UpdateSpaceLendingAttendeesRequest(attendees)))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /**
     * Leaves the notes and issues of the stay, with photos. The files are sent as they are, without touching their
     * contents, so all their metadata is kept. Only once the lending is over.
     */
    suspend fun submitReport(
        lendingId: Uuid,
        notes: String?,
        issues: String?,
        notesFiles: List<PlatformFile>,
        issuesFiles: List<PlatformFile>,
        progress: ProgressNotifier? = null,
    ) {
        val request = SubmitSpaceLendingReportRequest(
            reportNotes = notes?.takeIf { it.isNotBlank() },
            reportIssues = issues?.takeIf { it.isNotBlank() },
            reportNotesFiles = notesFiles.map { it.fileWithContext() },
            reportIssuesFiles = issuesFiles.map { it.fileWithContext() },
        )
        val response = httpClient.post(Api.SpaceLendings.Id.Report(id(lendingId))) {
            setBody(requestBody(request, SubmitSpaceLendingReportRequest.serializer()))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /** Attaches the confirmation of a bank transfer. */
    suspend fun attachPaymentProof(lendingId: Uuid, files: List<PlatformFile>, progress: ProgressNotifier? = null) {
        val request = AttachPaymentProofRequest(files.map { it.fileWithContext() })
        val response = httpClient.post(Api.SpaceLendings.Id.PaymentProof(id(lendingId))) {
            setBody(requestBody(request, AttachPaymentProofRequest.serializer()))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /** For managers: hands the keys over, which locks the lending. */
    suspend fun pickup(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post(Api.SpaceLendings.Id.Pickup(id(lendingId))) { monitorUploadProgress(progress) }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /** For managers: takes the keys back. */
    suspend fun returnKeys(lendingId: Uuid, progress: ProgressNotifier? = null) {
        val response = httpClient.post(Api.SpaceLendings.Id.Return(id(lendingId))) { monitorUploadProgress(progress) }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    /** For managers: sets the payment status. It's paid once the keys are back. */
    suspend fun setPayment(lendingId: Uuid, status: PaymentStatus, progress: ProgressNotifier? = null) {
        val response = httpClient.post(Api.SpaceLendings.Id.Payment(id(lendingId))) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SetSpaceLendingPaymentRequest.serializer(), SetSpaceLendingPaymentRequest(status)))
            monitorUploadProgress(progress)
        }
        if (!response.status.isSuccess()) throw response.bodyAsError().toThrowable()
        refresh(lendingId, progress)
    }

    override suspend fun insertRemoteEntity(entity: SpaceLending): SpaceLending {
        spaceLendingsRepository.upsert(entity)
        return entity
    }

    override suspend fun updateRemoteEntity(entity: SpaceLending): SpaceLending {
        spaceLendingsRepository.upsert(entity)
        return entity
    }

    override suspend fun upsertRemoteEntity(entity: SpaceLending): SpaceLending {
        spaceLendingsRepository.upsert(entity)
        return entity
    }
}
