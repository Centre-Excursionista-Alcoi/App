package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.ViaLink
import org.centrexcursionistalcoi.app.database.utils.list
import org.centrexcursionistalcoi.app.database.utils.serializer
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*
import kotlin.uuid.Uuid

object Events : UUIDTable("events"), ViaLink<UUID, EventEntity, String, UserReferenceEntity>, CustomTableSerializer<UUID, EventEntity> {
    val created = timestamp("created").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val start = timestamp("start")
    val end = timestamp("end").nullable()

    val place = text("place")

    val title = text("title")
    val description = text("description").nullable()

    val maxPeople = long("maxPeople").nullable()
    val requiresConfirmation = bool("requiresConfirmation").default(false)
    val requiresInsurance = bool("requiresInsurance").default(false)

    val department = optReference("department", Departments)
    val image = optReference("image", Files)


    override val linkName: String = "userReferences"

    override fun linkSerializer(): Pair<SerializationStrategy<UserReferenceEntity>, Boolean> =
        (UserReferenceEntity.serializer() to /* nullable */ false)

    override fun links(entity: EventEntity): SizedIterable<UserReferenceEntity> = entity.userReferences


    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "userSubList" to Uuid.serializer().list()
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: EventEntity): Map<String, Any?> = mapOf(
        "userSubList" to entity.userReferences.map { it.id.value }
    )
}
