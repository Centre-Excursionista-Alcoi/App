package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.Ignore
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
    val memberPrice: Double? = null,
    val externalPrice: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
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

    constructor(
        id: Int,
        createdAt: Instant,
        name: String,
        description: String?,
        capacity: Int?,
        memberPrice: MoneyD?,
        externalPrice: MoneyD?,
        location: Location?,
        address: Address?,
        images: List<ByteArray>?,
        keys: List<SpaceKeyD>?
    ): this(
        id,
        createdAt,
        name,
        description,
        capacity,
        memberPrice?.amount,
        externalPrice?.amount,
        location?.latitude,
        location?.longitude,
        address?.address,
        address?.city,
        address?.postalCode,
        address?.country,
        images,
        keys
    )

    @get:Ignore
    val location: Location?
        get() = if (latitude != null && longitude != null) Location(latitude, longitude) else null

    @get:Ignore
    val fullAddress: Address?
        get() = Address(address, city, postalCode, country).orNull()

    @OptIn(ExperimentalEncodingApi::class)
    override fun serializable(): SpaceD {
        return SpaceD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt.toEpochMilliseconds(),
            name = name,
            description = description,
            capacity = capacity,
            memberPrice = memberPrice?.let { MoneyD(amount = it) },
            externalPrice = externalPrice?.let { MoneyD(amount = it) },
            location = location,
            address = fullAddress,
            images = images?.map { Base64.encode(it) },
            keys = keys
        )
    }

    override fun validate(): Boolean {
        return serializable().validate()
    }
}
