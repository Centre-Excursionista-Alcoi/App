package org.centrexcursionistalcoi.app.database.utils

import io.ktor.util.reflect.instanceOf
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.serialization.InstantSerializer
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import org.jetbrains.exposed.v1.core.BasicBinaryColumnType
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.EntityIDColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.StringColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.UUIDColumnType
import org.jetbrains.exposed.v1.core.datetime.InstantColumnType
import org.jetbrains.exposed.v1.crypt.EncryptedVarCharColumnType
import org.jetbrains.exposed.v1.dao.DaoEntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.UIntEntity
import org.jetbrains.exposed.v1.dao.ULongEntity
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.javatime.JavaLocalDateColumnType

fun <ID : Any, E : Entity<ID>> Json.encodeEntityToString(entity: E, entityClass: EntityClass<ID, E>): String {
    return encodeToString(entityClass.serializer(), entity)
}

inline fun <ID : Any, reified E : Entity<ID>> Json.encodeEntityToString(entity: E): String {
    // Companion object must be the EntityClass
    val companion = E::class.companionObjectInstance
    requireNotNull(companion) { "Entity class does not have a companion" }
    require(companion.instanceOf(EntityClass::class)) { "Companion object is not sub-class of EntityClass" }

    @Suppress("UNCHECKED_CAST")
    companion as EntityClass<ID, E>

    return encodeEntityToString(entity, companion)
}

fun <ID : Any, E : Entity<ID>> Json.encodeEntityListToString(entities: List<E>, entityClass: EntityClass<ID, E>): String {
    return encodeToString(entityClass.serializer().list(), entities)
}

inline fun <ID : Any, reified E : Entity<ID>> Json.encodeEntityListToString(entities: List<E>): String {
    // If the list is empty, return an empty JSON array
    if (entities.isEmpty()) return "[]"

    // Companion object must be the EntityClass
    val companion = E::class.companionObjectInstance
    requireNotNull(companion) { "Entity class does not have a companion" }
    require(companion.instanceOf(EntityClass::class)) { "Companion object is not sub-class of EntityClass" }

    @Suppress("UNCHECKED_CAST")
    companion as EntityClass<ID, E>

    return encodeEntityListToString(entities, companion)
}

fun <ID : Any, E : Entity<ID>> EntityClass<ID, E>.serializer(): SerializationStrategy<E> {
    val className = if (this::class.isCompanion) this::class.java.enclosingClass.simpleName else this::class.simpleName
    val serialName = "org.centrexcursionistalcoi.app.database.entity.$className"

    return table.serializer(serialName)
}

private fun <ID : Any, E : Entity<ID>> Table.serializer(serialName: String): SerializationStrategy<E> {
    return object : KSerializer<E> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
            println("Columns for $tableName:")
            for (column in columns) {
                println("- ${column.name}, Type: ${column.columnType::class.simpleName}, Nullable: ${column.columnType.nullable}")
                when (val type = column.columnType) {
                    is EntityIDColumnType<*> -> element<String>(column.name, isOptional = type.nullable) // EntityIDs are serialized as Strings
                    is EncryptedVarCharColumnType -> continue // Encrypted columns should not be serialized
                    is StringColumnType -> element<String>(column.name, isOptional = type.nullable)
                    is BooleanColumnType -> element<Boolean>(column.name, isOptional = type.nullable)
                    is IntegerColumnType -> element<Int>(column.name, isOptional = type.nullable)
                    is DoubleColumnType -> element<Double>(column.name, isOptional = type.nullable)
                    is LongColumnType -> element<Long>(column.name, isOptional = type.nullable)
                    is InstantColumnType<*> -> element(column.name, InstantSerializer.descriptor, isOptional = type.nullable)
                    is JavaLocalDateColumnType -> element<String>(column.name, isOptional = type.nullable) // LocalDates are serialized as Strings
                    is UUIDColumnType -> element<String>(column.name, isOptional = type.nullable) // UUIDs are serialized as Strings
                    is BasicBinaryColumnType -> element(column.name, Base64Serializer.descriptor, isOptional = type.nullable) // ByteArrays are serialized as Base64 Strings
                    else -> throw IllegalArgumentException("Unsupported column type: ${column.columnType::class.simpleName}")
                }
            }
            if (this@serializer is ViaLink<*, *, *, *>) {
                val (serializer, nullable) = linkSerializer()
                println("Link: $linkName, Serializer: ${serializer.descriptor.serialName}, Nullable: $nullable")
                element(linkName, serializer.descriptor, isOptional = nullable)
            }
        }

        override fun serialize(encoder: Encoder, value: E) = Database {
            println("Encoding structure of ${value::class.simpleName} (${descriptor.serialName})...")
            println("Columns: ${columns.joinToString { it.name }}")
            encoder.encodeStructure(descriptor) {
                for (column in columns) {
                    val columnName = column.name
                    val idx = descriptor.getElementIndex(columnName)
                    val className = value::class.simpleName
                    val members = value::class.members
                    val member = members.find { it.name == columnName }
                    if (member == null) {
                        if (!column.columnType.nullable) {
                            error("Could not find member named \"$columnName\" in ${className}.\nMembers: ${members.joinToString { it.name }}")
                        }
                        println("- Won't encode column \"$columnName\" because member is missing.\n\tMembers: ${members.joinToString { it.name }}")
                        // Skip missing members
                        continue
                    }
                    val typeValue = member.call(value)
                    if (typeValue == null) {
                        if (!column.columnType.nullable) {
                            error("Could not find property or function named \"$columnName\" in ${className}.\nMembers: ${members.joinToString { it.name }}")
                        }
                        println("- Won't encode column \"$columnName\" because it's null")
                        // Skip null values
                        continue
                    }
                    println("- Encoding column \"$columnName\" (Type: ${column.columnType::class.simpleName}) with value: $typeValue")
                    when (column.columnType) {
                        is EntityIDColumnType<*> -> {
                            when (typeValue) {
                                is DaoEntityID<*> -> encodeStringElement(descriptor, idx, typeValue.value.toString())
                                is IntEntity -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                is LongEntity -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                is UIntEntity -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                is ULongEntity -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                is UUIDEntity -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                is Entity<*> -> encodeStringElement(descriptor, idx, typeValue.id.value.toString())
                                else -> error("Unsupported entity ID column type: ${typeValue::class.simpleName}")
                            }
                        }
                        is EncryptedVarCharColumnType -> {
                            // Encrypted columns should not be serialized
                            continue
                        }
                        is StringColumnType -> {
                            encodeStringElement(descriptor, idx, typeValue as String)
                        }
                        is BooleanColumnType -> {
                            encodeBooleanElement(descriptor, idx, typeValue as Boolean)
                        }
                        is IntegerColumnType -> {
                            encodeIntElement(descriptor, idx, typeValue as Int)
                        }
                        is DoubleColumnType -> {
                            encodeDoubleElement(descriptor, idx, typeValue as Double)
                        }
                        is LongColumnType -> {
                            encodeLongElement(descriptor, idx, typeValue as Long)
                        }
                        is InstantColumnType<*> -> {
                            encodeSerializableElement(descriptor, idx, InstantSerializer, typeValue as Instant)
                        }
                        is JavaLocalDateColumnType -> {
                            encodeStringElement(descriptor, idx, (typeValue as LocalDate).toString())
                        }
                        is UUIDColumnType -> {
                            encodeStringElement(descriptor, idx, (typeValue as UUID).toString())
                        }
                        is BasicBinaryColumnType -> {
                            encodeSerializableElement(descriptor, idx, Base64Serializer, typeValue as ByteArray)
                        }
                        else -> throw IllegalArgumentException("Unsupported column type: ${column.columnType::class.simpleName}")
                    }
                }
                if (this@serializer is ViaLink<*, *, *, *>) { // ViaLink<ID, E, *, *>
                    @Suppress("UNCHECKED_CAST")
                    with(this@serializer as ViaLink<ID, E, *, *>) {
                        encodeViaLink(descriptor, value)
                    }
                }
            }
        }

        override fun deserialize(decoder: Decoder): E {
            throw UnsupportedOperationException("Deserialization of entities is not supported. Data must be fetched from the database")
        }
    }
}

fun <T> SerializationStrategy<T>.list(): SerializationStrategy<List<T>> {
    return ListSerializer(this as KSerializer<T>)
}

context(viaLink: ViaLink<FromID, FromEntity, ToID, ToEntity>)
private fun <FromID: Any, FromEntity: Entity<FromID>, ToID: Any, ToEntity: Entity<ToID>> CompositeEncoder.encodeViaLink(
    descriptor: SerialDescriptor,
    value: FromEntity,
) {
    val (serializer, nullable) = viaLink.linkSerializer()
    val links = viaLink.links(value)
    val idx = descriptor.getElementIndex(viaLink.linkName)
    if (links.empty() && nullable) {
        // Skip empty links if nullable
        return
    }
    encodeSerializableElement(descriptor, idx, serializer.list(), links.toList())
}
