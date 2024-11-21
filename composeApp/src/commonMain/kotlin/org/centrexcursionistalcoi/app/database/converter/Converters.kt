package org.centrexcursionistalcoi.app.database.converter

import androidx.room.TypeConverter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceKeyD
import org.centrexcursionistalcoi.app.serverJson

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }


    @TypeConverter
    fun fromDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun instantToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }


    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")?.map { it.toInt() }
    }

    @TypeConverter
    fun intListToString(list: List<Int>?): String? {
        return list?.joinToString(",")
    }


    @TypeConverter
    @OptIn(ExperimentalEncodingApi::class)
    fun fromByteArrayList(value: String?): List<ByteArray>? {
        return value?.split("\r")?.map { Base64.decode(it) }
    }

    @TypeConverter
    @OptIn(ExperimentalEncodingApi::class)
    fun byteArrayListToString(list: List<ByteArray>?): String? {
        return list?.joinToString("\r") { Base64.encode(it) }
    }


    @TypeConverter
    fun fromSpaceKeyList(value: String?): List<SpaceKeyD>? {
        return value?.let {
            serverJson.decodeFromString(ListSerializer(SpaceKeyD.serializer()), it)
        }
    }

    @TypeConverter
    fun spaceKeyListToString(list: List<SpaceKeyD>?): String? {
        return list?.let {
            serverJson.encodeToString(ListSerializer(SpaceKeyD.serializer()), it)
        }
    }
}
