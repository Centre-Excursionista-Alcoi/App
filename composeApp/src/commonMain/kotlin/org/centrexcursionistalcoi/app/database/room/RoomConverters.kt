package org.centrexcursionistalcoi.app.database.room

import androidx.room3.ColumnTypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.json
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomConverters {
    @ColumnTypeConverter
    fun uuidToString(value: Uuid?): String? = value?.toString()

    @ColumnTypeConverter
    fun stringToUuid(value: String?): Uuid? = value?.let { Uuid.parse(it) }

    @ColumnTypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @ColumnTypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @ColumnTypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @ColumnTypeConverter
    fun zonedDateTimeToString(value: ZonedDateTime?): String? = value?.toString()

    @ColumnTypeConverter
    fun stringToZonedDateTime(value: String?): ZonedDateTime? = value?.let { ZonedDateTime.parse(it) }

    @ColumnTypeConverter
    fun sportsToString(value: Sports?): String? = value?.name

    @ColumnTypeConverter
    fun stringToSports(value: String?): Sports? = value?.let { Sports.valueOf(it) }

    @ColumnTypeConverter
    fun memberStatusToString(value: Member.Status?): String? = value?.name

    @ColumnTypeConverter
    fun stringToMemberStatus(value: String?): Member.Status? = value?.let { Member.Status.valueOf(it) }

    @ColumnTypeConverter
    fun stringListToString(value: List<String>?): String? =
        value?.let { json.encodeToString(ListSerializer(String.serializer()), it) }

    @ColumnTypeConverter
    fun stringToStringList(value: String?): List<String>? =
        value?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }

    @ColumnTypeConverter
    fun uIntListToString(value: List<UInt>?): String? =
        value?.let { json.encodeToString(ListSerializer(UInt.serializer()), it) }

    @ColumnTypeConverter
    fun stringToUIntList(value: String?): List<UInt>? =
        value?.let { json.decodeFromString(ListSerializer(UInt.serializer()), it) }

    @ColumnTypeConverter
    fun uuidListToString(value: List<Uuid>?): String? =
        value?.let { json.encodeToString(ListSerializer(Uuid.serializer()), it) }

    @ColumnTypeConverter
    fun stringToUuidList(value: String?): List<Uuid>? =
        value?.let { json.decodeFromString(ListSerializer(Uuid.serializer()), it) }

    @ColumnTypeConverter
    fun departmentMemberInfoListToString(value: List<DepartmentMemberInfo>?): String? =
        value?.let { json.encodeToString(ListSerializer(DepartmentMemberInfo.serializer()), it) }

    @ColumnTypeConverter
    fun stringToDepartmentMemberInfoList(value: String?): List<DepartmentMemberInfo>? =
        value?.let { json.decodeFromString(ListSerializer(DepartmentMemberInfo.serializer()), it) }

    @ColumnTypeConverter
    fun userInsuranceListToString(value: List<UserInsurance>?): String? =
        value?.let { json.encodeToString(ListSerializer(UserInsurance.serializer()), it) }

    @ColumnTypeConverter
    fun stringToUserInsuranceList(value: String?): List<UserInsurance>? =
        value?.let { json.decodeFromString(ListSerializer(UserInsurance.serializer()), it) }

    @ColumnTypeConverter
    fun lendingUserToString(value: LendingUser?): String? =
        value?.let { json.encodeToString(LendingUser.serializer(), it) }

    @ColumnTypeConverter
    fun stringToLendingUser(value: String?): LendingUser? =
        value?.let { json.decodeFromString(LendingUser.serializer(), it) }

    @ColumnTypeConverter
    fun fileWithContextListToString(value: List<FileWithContext>?): String? =
        value?.let { json.encodeToString(ListSerializer(FileWithContext.serializer()), it) }

    @ColumnTypeConverter
    fun stringToFileWithContextList(value: String?): List<FileWithContext>? =
        value?.let { json.decodeFromString(ListSerializer(FileWithContext.serializer()), it) }
}
