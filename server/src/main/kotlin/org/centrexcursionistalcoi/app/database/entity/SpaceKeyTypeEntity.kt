package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeyTypeSpaces
import org.centrexcursionistalcoi.app.database.table.SpaceKeyTypes
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyTypeRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.utils.takeUnlessEmpty
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SpaceKeyTypeEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<SpaceKeyType, Uuid>, EntityPatcher<UpdateSpaceKeyTypeRequest> {
    override var lastUpdate: Instant by SpaceKeyTypes.lastUpdate

    var name: String by SpaceKeyTypes.name
    var description: String? by SpaceKeyTypes.description

    /** The spaces, loaded ahead for a whole list, see [withDataPreloaded]. */
    private var preloadedSpaces: List<SpaceKeyTypeSpace>? = null

    context(_: JdbcTransaction)
    fun spaces(): List<SpaceKeyTypeSpace> = preloadedSpaces
        ?: SpaceKeyTypeSpaces.selectAll().where { SpaceKeyTypeSpaces.keyType eq id }.map { it.toLink() }

    /** Replaces the spaces this type is for. */
    context(_: JdbcTransaction)
    fun setSpaces(spaces: List<SpaceKeyTypeSpace>) {
        SpaceKeyTypeSpaces.deleteWhere { keyType eq this@SpaceKeyTypeEntity.id }
        for (link in spaces.distinctBy { it.space }) {
            SpaceKeyTypeSpaces.insert {
                it[keyType] = this@SpaceKeyTypeEntity.id
                it[space] = link.space
                it[maxPerLending] = link.maxPerLending
            }
        }
        preloadedSpaces = null
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    override fun toData(): SpaceKeyType = SpaceKeyType(
        id = id.value,
        name = name,
        description = description,
        spaces = spaces(),
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateSpaceKeyTypeRequest) {
        request.name?.takeUnlessEmpty()?.let { name = it }
        // An empty string clears it
        request.description?.let { description = it.takeUnlessEmpty() }
        request.spaces?.let {
            validateSpaces(it)
            setSpaces(it)
        }
    }

    companion object : UuidEntityClass<SpaceKeyTypeEntity>(SpaceKeyTypes) {
        private fun ResultRow.toLink() = SpaceKeyTypeSpace(
            space = this[SpaceKeyTypeSpaces.space].value,
            maxPerLending = this[SpaceKeyTypeSpaces.maxPerLending],
        )

        /** Checks that the spaces exist and the maximums make sense. */
        context(_: JdbcTransaction)
        fun validateSpaces(spaces: List<SpaceKeyTypeSpace>) {
            for (link in spaces) {
                require(link.maxPerLending > 0) { "maxPerLending must be positive" }
                SpaceEntity.findById(link.space) ?: throw NoSuchElementException("Space with id ${link.space} does not exist")
            }
        }

        /**
         * Loads, for all of [types] at once, the spaces they are for. Instead, each would run its own query. Only holds
         * while the caller stays in the current transaction.
         */
        context(_: JdbcTransaction)
        fun withDataPreloaded(types: List<SpaceKeyTypeEntity>): List<SpaceKeyTypeEntity> {
            if (types.isEmpty()) return types
            val links = SpaceKeyTypeSpaces.selectAll().where { SpaceKeyTypeSpaces.keyType inList types.map { it.id } }
                .groupBy({ it[SpaceKeyTypeSpaces.keyType].value }, { it.toLink() })
            for (type in types) type.preloadedSpaces = links[type.id.value].orEmpty()
            return types
        }
    }
}
