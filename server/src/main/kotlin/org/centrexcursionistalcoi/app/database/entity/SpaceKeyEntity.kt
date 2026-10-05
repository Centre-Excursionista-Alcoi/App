package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeys
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.utils.takeUnlessEmpty
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SpaceKeyEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<SpaceKey, Uuid>, EntityPatcher<UpdateSpaceKeyRequest> {
    override var lastUpdate: Instant by SpaceKeys.lastUpdate

    var space by SpaceEntity referencedOn SpaceKeys.space
    var name: String by SpaceKeys.name
    var maxQuantity: Int by SpaceKeys.maxQuantity
    var nfcId by SpaceKeys.nfcId

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    override fun toData(): SpaceKey = SpaceKey(
        id = id.value,
        lastUpdate = lastUpdate,
        space = space.id.value,
        name = name,
        maxQuantity = maxQuantity,
        nfcId = nfcId,
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateSpaceKeyRequest) {
        request.name?.takeUnlessEmpty()?.let { name = it }
        request.maxQuantity?.let { maxQuantity = it }
        request.nfcId?.let { nfcId = it.takeUnless { id -> id.isEmpty() } }
    }

    companion object : UuidEntityClass<SpaceKeyEntity>(SpaceKeys)
}
