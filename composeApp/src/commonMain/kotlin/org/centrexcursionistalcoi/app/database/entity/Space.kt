package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.Address
import org.centrexcursionistalcoi.app.data.Location
import org.centrexcursionistalcoi.app.data.MoneyD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.SpaceKeyD

@Entity
data class Space(
    @PrimaryKey override val id: Int = 0,
    override val createdAt: Instant = Clock.System.now(),
    val name: String = "",
    val description: String? = null,
    val capacity: Int? = null,
    @Embedded(prefix = "member") val memberPrice: MoneyD? = null,
    @Embedded(prefix = "external") val externalPrice: MoneyD? = null,
    @Embedded val location: Location? = null,
    @Embedded val address: Address? = null,
    val images: List<ByteArray>? = null,
    val keys: List<SpaceKeyD>? = null
): DatabaseEntity<SpaceD> {
    companion object : EntityDeserializer<SpaceD, Space> {
        @OptIn(ExperimentalEncodingApi::class)
        override fun deserialize(source: SpaceD): Space {
            return Space(
                id = source.id!!,
                createdAt = Instant.fromEpochMilliseconds(source.createdAt!!),
                name = source.name,
                description = source.description,
                capacity = source.capacity,
                memberPrice = source.memberPrice,
                externalPrice = source.externalPrice,
                location = source.location,
                address = source.address,
                images = source.images?.map { Base64.decode(it) },
                keys = source.keys
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun serializable(): SpaceD {
        return SpaceD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt.toEpochMilliseconds(),
            name = name,
            description = description,
            capacity = capacity,
            memberPrice = memberPrice,
            externalPrice = externalPrice,
            location = location,
            address = address,
            images = images?.map { Base64.encode(it) },
            keys = keys
        )
    }

    override fun validate(): Boolean {
        return serializable().validate()
    }
}
