package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.Spaces
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.utils.takeUnlessEmpty
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SpaceEntity(id: EntityID<Uuid>): UuidEntity(id), LastUpdateEntity, EntityDataConverter<Space, Uuid>, EntityPatcher<UpdateSpaceRequest> {
    override var lastUpdate: Instant by Spaces.lastUpdate

    var name: String by Spaces.name
    var description: String by Spaces.description
    var conditionsOfUse: String? by Spaces.conditionsOfUse
    var prices: List<CategoryPrice> by Spaces.prices

    var requiresKeys by Spaces.requiresKeys

    var isClosed by Spaces.isClosed
    var closedSince by Spaces.closedSince
    var closedUntil by Spaces.closedUntil
    var closedReason by Spaces.closedReason

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    override fun toData(): Space = Space(
        id = id.value,
        lastUpdate = lastUpdate,
        name = name,
        description = description,
        conditionsOfUse = conditionsOfUse,
        prices = prices,
        requiresKeys = requiresKeys,
        isClosed = isClosed,
        closedSince = closedSince,
        closedUntil = closedUntil,
        closedReason = closedReason,
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateSpaceRequest) {
        request.name?.takeUnlessEmpty()?.let { name = it }
        request.description?.takeUnlessEmpty()?.let { description = it }
        // An empty string clears them
        request.conditionsOfUse?.let { conditionsOfUse = it.takeUnlessEmpty() }
        request.prices?.let { prices = it }
        request.requiresKeys?.let { requiresKeys = it }
        request.closedReason?.let { closedReason = it.takeUnlessEmpty() }
        request.closedUntil?.let { closedUntil = it }
        request.closedSince?.let { closedSince = it }
        request.isClosed?.let { closed ->
            isClosed = closed
            if (closed) {
                if (closedSince == null) closedSince = now()
            } else {
                closedSince = null
                closedUntil = null
                closedReason = null
            }
        }
    }

    companion object : UuidEntityClass<SpaceEntity>(Spaces)
}
