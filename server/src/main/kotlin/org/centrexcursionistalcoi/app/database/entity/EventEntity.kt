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
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id), LastUpdateEntity, EntityDataConverter<Event, Uuid>, EntityPatcher<UpdateEventRequest> {
    companion object : UUIDEntityClass<EventEntity>(Events) {
        private val logger = LoggerFactory.getLogger("EventEntity")

        /**
         * How long an event without an end date is taken to last, when deciding whether it's over. A whole day
         * always covers the rest of the day it starts on, in whatever time zone, so a client can trim the list to
         * the exact end of that day in its own zone without the server having to know it.
         */
        private val WITHOUT_END_LASTS: Duration = Duration.ofDays(1)

        /**
         * The events that aren't over yet at [now] -- still to come or in progress -- as a query condition. Must
         * agree with [isNotOverAt].
         */
        private fun notOverAt(now: Instant): Op<Boolean> =
            (Events.end greaterEq now) or (Events.end.isNull() and (Events.start greaterEq now.minus(WITHOUT_END_LASTS)))

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
    private fun isNotOverAt(now: Instant): Boolean = (end ?: start.plus(WITHOUT_END_LASTS)) >= now

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
    fun qualificationRequirements(): List<List<UUID>> =
        EventQualificationRequirements.selectAll()
            .where { EventQualificationRequirements.event eq id }
            .groupBy({ it[EventQualificationRequirements.groupIndex] }, { it[EventQualificationRequirements.qualification].value })
            .toSortedMap()
            .values
            .map { it.sortedBy(UUID::toString) }

    /**
     * Replaces this event's qualification requirements with [groups], which must have already been checked with
     * [validatedQualificationRequirements].
     */
    context(_: JdbcTransaction)
    fun setQualificationRequirements(groups: List<List<UUID>>) {
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

    /**
     * Deletes the event together with its attendee list: [EventMembers] has no `ON DELETE CASCADE` on its event
     * reference, so deleting an event with confirmed attendees would otherwise violate its foreign key. Qualification
     * requirements are removed by their own cascading reference.
     */
    override fun delete() {
        EventMembers.deleteWhere { EventMembers.event eq this@EventEntity.id }
        super.delete()
    }

    context(_: JdbcTransaction)
    override fun toData(): Event = Event(
        id = id.value.toKotlinUuid(),
        start = start.toKotlinInstant(),
        end = end?.toKotlinInstant(),
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        requiresInsurance = requiresInsurance,
        department = department?.id?.value?.toKotlinUuid(),
        image = image?.id?.value?.toKotlinUuid(),
        userSubList = userReferences.map { it.sub.value },
        qualificationRequirements = qualificationRequirements().map { group -> group.map { it.toKotlinUuid() } },
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateEventRequest) {
        request.start?.let { start = it.toJavaInstant() }
        request.end?.let { end = it.toJavaInstant() }
        request.place?.let { place = it }
        request.title?.let { title = it }
        request.description?.let { description = it }
        request.maxPeople?.let { maxPeople = it }
        request.requiresConfirmation?.let { requiresConfirmation = it }
        request.requiresInsurance?.let { requiresInsurance = it }
        request.department?.let { department = DepartmentEntity.findById(it.toJavaUuid()) }
        request.image?.let { image = FileEntity.updateOrCreate(it) }

        // Requirements can only be on the event's own department's qualifications, so they have to be checked
        // against the department the event ends up in -- which also catches moving an event that already has
        // requirements to another department without replacing them.
        if (request.qualificationRequirements != null || request.department != null) {
            val requested = request.qualificationRequirements?.map { group -> group.map { it.toJavaUuid() } }
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
            eventId = this@EventEntity.id.value.toKotlinUuid(),
            userSub = session.sub,
            isConfirmed = true,
        )
    }

    fun assistanceRejectedNotification(session: UserSession): PushNotification.EventAssistanceUpdated = Database {
        PushNotification.EventAssistanceUpdated(
            eventId = this@EventEntity.id.value.toKotlinUuid(),
            userSub = session.sub,
            isConfirmed = false,
        )
    }
}
