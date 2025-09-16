package org.centrexcursionistalcoi.app.database.utils

import io.ktor.util.reflect.instanceOf
import java.time.Instant
import java.util.UUID
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.serialization.InstantSerializer
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.EntityIDColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.StringColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.UUIDColumnType
import org.jetbrains.exposed.v1.core.datetime.InstantColumnType
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

fun <ID : Any, E : Entity<ID>> Json.encodeListToString(entities: List<E>, entityClass: EntityClass<ID, E>): String {
    return encodeToString(entityClass.serializer().list(), entities)
}

inline fun <ID : Any, reified E : Entity<ID>> Json.encodeListToString(entities: List<E>): String {
    // If the list is empty, return an empty JSON array
    if (entities.isEmpty()) return "[]"

    // Companion object must be the EntityClass
    val companion = E::class.companionObjectInstance
    requireNotNull(companion) { "Entity class does not have a companion" }
    require(companion.instanceOf(EntityClass::class)) { "Companion object is not sub-class of EntityClass" }

    @Suppress("UNCHECKED_CAST")
    companion as EntityClass<ID, E>

    return encodeListToString(entities, companion)
}

fun <ID : Any, E : Entity<ID>> EntityClass<ID, E>.serializer(): SerializationStrategy<E> {
    val className = if (this::class.isCompanion) this::class.java.enclosingClass.simpleName else this::class.simpleName
    val serialName = "org.centrexcursionistalcoi.app.database.entity.$className"

    return table.serializer(serialName)
}

private fun <ID : Any, E : Entity<ID>> Table.serializer(serialName: String): SerializationStrategy<E> {
    return object : KSerializer<E> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
            for (column in columns) {
                when (val type = column.columnType) {
                    is EntityIDColumnType<*> -> element<String>(column.name) // EntityIDs are serialized as Strings
                    is StringColumnType -> element<String>(column.name, isOptional = type.nullable)
                    is BooleanColumnType -> element<Boolean>(column.name, isOptional = type.nullable)
                    is IntegerColumnType -> element<Int>(column.name, isOptional = type.nullable)
                    is DoubleColumnType -> element<Double>(column.name, isOptional = type.nullable)
                    is LongColumnType -> element<Long>(column.name, isOptional = type.nullable)
                    is InstantColumnType<*> -> element(column.name, InstantSerializer.descriptor, isOptional = type.nullable)
                    is UUIDColumnType -> element<String>(column.name, isOptional = type.nullable) // UUIDs are serialized as Strings
                    else -> throw IllegalArgumentException("Unsupported column type: ${column.columnType::class.simpleName}")
                }
            }
        }

        override fun serialize(encoder: Encoder, value: E) {
            encoder.encodeStructure(descriptor) {
                for (column in columns) {
                    val columnName = column.name
                    val idx = descriptor.getElementIndex(columnName)
                    val typeValue = value.run {
                        this::class.members.find { it.name == columnName }
                            ?.call(value)
                            ?: error("Could not find property or function named \"$columnName\" in ${this::class.simpleName}.\nMembers: ${this::class.members.joinToString { it.name }}")
                    }
                    when (column.columnType) {
                        is EntityIDColumnType<*> -> {
                            encodeStringElement(descriptor, idx, typeValue.toString())
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
                        is UUIDColumnType -> {
                            encodeStringElement(descriptor, idx, (typeValue as UUID).toString())
                        }
                        else -> throw IllegalArgumentException("Unsupported column type: ${column.columnType::class.simpleName}")
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
