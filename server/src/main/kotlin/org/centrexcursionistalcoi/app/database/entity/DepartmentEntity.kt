package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.ImageContainerEntity
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentEntity(id: EntityID<UUID>) : UUIDEntity(id), LastUpdateEntity, EntityDataConverter<Department, Uuid>, EntityPatcher<UpdateDepartmentRequest>, ImageContainerEntity {
    companion object : UUIDEntityClass<DepartmentEntity>(Departments)

    override var lastUpdate by Departments.lastUpdate

    var displayName by Departments.displayName
    override var image by FileEntity optionalReferencedOn Departments.image

    val members by DepartmentMemberEntity referrersOn DepartmentMembers.departmentId

    val confirmedMembers get() = members.filter { it.confirmed }

    /**
     * The subset of [members] visible to [session]: everyone (including pending/unconfirmed requests) for an
     * admin or a confirmed `PEOPLE_MANAGER` of this department, otherwise just the caller's own row (or none,
     * for an anonymous caller or a non-member). This is the single source of truth for that rule -- both
     * `GET /departments/{id}` (via `Departments.extraColumns`) and `GET /departments/{id}/members`
     * (`DepartmentRoutes.kt`) call this rather than each re-implementing it, so they can't silently diverge.
     *
     * Reuses the already-loaded [members] collection instead of issuing a separate department-role query, so
     * this stays a single query per department (needed for [members] regardless) even when serializing a whole
     * list of departments.
     */
    context(_: JdbcTransaction)
    fun visibleMembersFor(session: UserSession?): List<DepartmentMemberEntity> {
        if (session == null) return emptyList()
        val allMembers = members.toList()
        val ownMembership = allMembers.find { it.userReference.sub.value == session.sub }
        val isPeopleManager = ownMembership?.confirmed == true && ownMembership.hasRole(DepartmentRole.PEOPLE_MANAGER)
        return if (session.isAdmin() || isPeopleManager) allMembers else listOfNotNull(ownMembership)
    }

    context(_: JdbcTransaction)
    override fun toData(): Department = Department(
        id = id.value.toKotlinUuid(),
        displayName = displayName,
        image = image?.id?.value?.toKotlinUuid(),
        members = members.map { it.toData() },
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateDepartmentRequest) {
        request.displayName?.let { displayName = it }
        updateOrSetImage(request.image)
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }
}
