package org.centrexcursionistalcoi.app.database.utils

import java.time.Instant
import java.util.UUID
import javax.sound.sampled.BooleanControl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
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

fun <ID : Any, E : Entity<ID>> EntityClass<ID, E>.serializer(table: Table): KSerializer<E> {
    val className = if (this::class.isCompanion) this::class.java.enclosingClass.simpleName else this::class.simpleName
    val serialName = "org.centrexcursionistalcoi.app.database.entity.$className"

    return object : KSerializer<E> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
            for (column in table.columns) {
                when (column.columnType) {
                    is EntityIDColumnType<*> -> element<String>(column.name) // EntityIDs are serialized as Strings
                    is StringColumnType -> element<String>(column.name)
                    is BooleanColumnType -> element<Boolean>(column.name)
                    is IntegerColumnType -> element<Int>(column.name)
                    is DoubleColumnType -> element<Double>(column.name)
                    is LongColumnType -> element<Long>(column.name)
                    is InstantColumnType<*> -> element<Instant>(column.name)
                    is UUIDColumnType -> element<String>(column.name) // UUIDs are serialized as Strings
                    else -> throw IllegalArgumentException("Unsupported column type: ${column.columnType::class.simpleName}")
                }
            }
        }

        override fun serialize(encoder: Encoder, value: E) {
            encoder.encodeStructure(descriptor) {
                for (column in table.columns) {
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
