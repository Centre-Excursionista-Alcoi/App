package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.EventEntity.Companion.forSession
import org.centrexcursionistalcoi.app.database.entity.EventEntity.Companion.notOverAt
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.EventMembers
import org.centrexcursionistalcoi.app.database.table.EventQualificationRequirements
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.routes.PatchRejectedException
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.security.InvalidQualificationRequirementsException
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.validatedQualificationRequirements
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

class EventEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<Event, Uuid>, EntityPatcher<UpdateEventRequest> {
    companion object : UuidEntityClass<EventEntity>(Events) {
        private fun groupedRequirements(rows: List<ResultRow>): List<List<Uuid>> = rows
            .groupBy({ it[EventQualificationRequirements.groupIndex] }, { it[EventQualificationRequirements.qualification].value })
            .toSortedMap()
            .values
            .map { it.sortedBy(Uuid::toString) }

        /**
         * Loads, for all of [events] at once, what converting them needs: who confirmed assistance and the
         * requirements. Instead, each would run its own queries. Only holds while the caller stays in the current
         * transaction.
         */
        context(_: JdbcTransaction)
        fun withDataPreloaded(events: List<EventEntity>): List<EventEntity> {
            if (events.isEmpty()) return events
            events.with(EventEntity::userReferences)
            val rows = EventQualificationRequirements.selectAll()
                .where { EventQualificationRequirements.event inList events.map { it.id } }
                .groupBy { it[EventQualificationRequirements.event].value }
            for (event in events) event.preloadedRequirements = groupedRequirements(rows[event.id.value].orEmpty())
            return events
        }

        private val logger = LoggerFactory.getLogger("EventEntity")

        /**
         * How long an event without an end date is taken to last, when deciding whether it's over. A whole day
         * always covers the rest of the day it starts on, in whatever time zone, so a client can trim the list to
         * the exact end of that day in its own zone without the server having to know it.
         */
        private val WITHOUT_END_LASTS: Duration = 1.days

        /**
         * The events that aren't over yet at [now] -- still to come or in progress -- as a query condition. Must
         * agree with [isNotOverAt].
         */
        private fun notOverAt(now: Instant): Op<Boolean> =
            (Events.end greaterEq now) or (Events.end.isNull() and (Events.start greaterEq now - WITHOUT_END_LASTS))

        context(_: JdbcTransaction)
        fun forSession(session: UserSession?) = when {
            session == null -> {
                // Not logged in, only show public events (without department)
                logger.debug("Unauthenticated user, fetching public events...")
                find { Events.department eq null }
            }

            session.isAdmin() -> {
                // If admin, show all events
                logger.debug("Admin user ${session.sub} fetching all events...")
                all()
            }

            else -> {
                // Logged in, show public events, and events for the user's department
                logger.debug("Fetching events for user ${session.sub}")
                logger.debug("Fetching user departments for user ${session.sub}")
                val userDepartments = transaction {
                    DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                        .map { it.department.id.value }
                }
                logger.debug("User {} is in departments {}. Fetching events...", session.sub, userDepartments)
                val now = now()
                find {
                    notOverAt(now) and ((Events.department eq null) or (Events.department inList userDepartments))
                }
            }
        }
    }

    /**
     * Whether this single event is visible to [session] -- must stay in sync with [forSession], which is the
     * same rule applied at the list level. Evaluated directly against this entity's own fields (one department
     * lookup for the caller, not a query over every event), so this is safe to call per single-item GET.
     */
    context(_: JdbcTransaction)
    fun isVisibleTo(session: UserSession?): Boolean = when {
        session == null -> department == null
        session.isAdmin() -> true
        else -> {
            val eventDepartmentId = department?.id?.value
            isNotOverAt(now()) && (eventDepartmentId == null || DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true).any { it.department.id.value == eventDepartmentId })
        }
    }

    /**
     * Whether this event is still to come or in progress at [now]: it hasn't reached its end date or, without one,
     * a day after it started. Must agree with the query condition [notOverAt] used by [forSession].
     */
    private fun isNotOverAt(now: Instant): Boolean = (end ?: start + WITHOUT_END_LASTS) >= now

    val created by Events.created
    override var lastUpdate by Events.lastUpdate

    var start by Events.start
    var end by Events.end

    var place by Events.place

    var title by Events.title
    var description by Events.description

    var maxPeople by Events.maxPeople
    var requiresConfirmation by Events.requiresConfirmation
    var requiresInsurance by Events.requiresInsurance

    var department by DepartmentEntity optionalReferencedOn Events.department
    var image by FileEntity optionalReferencedOn Events.image

    val userReferences by UserReferenceEntity via EventMembers

    /**
     * The qualifications required to confirm assistance, as groups of alternatives that must all be satisfied
     * (see [Event.qualificationRequirements]).
     */
    context(_: JdbcTransaction)
    fun qualificationRequirements(): List<List<Uuid>> = preloadedRequirements ?: groupedRequirements(
        EventQualificationRequirements.selectAll().where { EventQualificationRequirements.event eq id }.toList()
    )

    /** The requirements, loaded ahead for a whole list, see [withDataPreloaded]. */
    private var preloadedRequirements: List<List<Uuid>>? = null

    /**
     * Replaces this event's qualification requirements with [groups], which must have already been checked with
     * [validatedQualificationRequirements].
     */
    context(_: JdbcTransaction)
    fun setQualificationRequirements(groups: List<List<Uuid>>) {
        preloadedRequirements = null
        EventQualificationRequirements.deleteWhere { EventQualificationRequirements.event eq this@EventEntity.id }
        groups.forEachIndexed { index, group ->
            for (qualificationId in group) {
                EventQualificationRequirements.insert {
                    it[event] = this@EventEntity.id
                    it[groupIndex] = index
                    it[qualification] = qualificationId
                }
            }
        }
    }

    context(_: JdbcTransaction)
    override fun toData(): Event = Event(
        id = id.value,
        start = start,
        end = end,
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        requiresInsurance = requiresInsurance,
        department = Events.department.lookup()?.value,
        image = Events.image.lookup()?.value,
        userSubList = userReferences.map { it.sub.value },
        qualificationRequirements = qualificationRequirements(),
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateEventRequest) {
        request.start?.let { start = it }
        request.end?.let { end = it }
        request.place?.let { place = it }
        request.title?.let { title = it }
        request.description?.let { description = it }
        request.maxPeople?.let { maxPeople = it }
        request.requiresConfirmation?.let { requiresConfirmation = it }
        request.requiresInsurance?.let { requiresInsurance = it }
        request.department?.let { department = DepartmentEntity.findById(it) }
        request.image?.let { request ->
            val oldImage = image
            val ownedFileIds = listOfNotNull(oldImage?.id?.value)
            // Unlink the image before it's deleted (if asked to remove it): events don't set their image to null on delete
            val newImage = FileEntity.updateOrCreate(request, ownedFileIds) { image = null; this@EventEntity.flush() }
            if (newImage != null) {
                image = newImage
                // Replaced, delete the old image
                if (oldImage != null) {
                    this@EventEntity.flush()
                    FileEntity.deleteIfUnreferenced(oldImage)
                }
            }
        }

        // Requirements can only be on the event's own department's qualifications, so they have to be checked
        // against the department the event ends up in -- which also catches moving an event that already has
        // requirements to another department without replacing them.
        if (request.qualificationRequirements != null || request.department != null) {
            val requested = request.qualificationRequirements
            val requirements = try {
                validatedQualificationRequirements(department?.id?.value, requested ?: qualificationRequirements())
            } catch (e: InvalidQualificationRequirementsException) {
                throw PatchRejectedException(Error.InvalidArgument("qualificationRequirements", e.message))
            }
            if (requested != null) setQualificationRequirements(requirements)
        }
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    fun assistanceConfirmedNotification(session: UserSession): PushNotification.EventAssistanceUpdated = Database {
        PushNotification.EventAssistanceUpdated(
            eventId = this@EventEntity.id.value,
            userSub = session.sub,
            isConfirmed = true,
        )
    }

    fun assistanceRejectedNotification(session: UserSession): PushNotification.EventAssistanceUpdated = Database {
        PushNotification.EventAssistanceUpdated(
            eventId = this@EventEntity.id.value,
            userSub = session.sub,
            isConfirmed = false,
        )
    }

    override fun delete() {
        val image = image
        super.delete()
        FileEntity.deleteOwnedFiles(listOfNotNull(image))
    }
}
