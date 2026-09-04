package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.Member

@Entity(tableName = "Members")
data class MemberEntity(
    @PrimaryKey
    val memberNumber: Long,
    val status: Member.Status?,
    val fullName: String,
    val nif: String?,
    val email: String?,
) {
    fun toMember() = Member(
        memberNumber = memberNumber.toUInt(),
        status = status,
        fullName = fullName,
        nif = nif,
        email = email,
    )

    companion object {
        fun Member.toEntity() = MemberEntity(
            memberNumber = memberNumber.toLong(),
            status = status,
            fullName = fullName,
            nif = nif,
            email = email,
        )
    }
}
