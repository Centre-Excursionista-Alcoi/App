package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UIntEntity
import org.jetbrains.exposed.v1.dao.UIntEntityClass

class MemberEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<MemberEntity>(Members)

    val memberNumber get() = id.value

    var status by Members.status

    var fullName by Members.fullName
    var nif by Members.nif
    var email by Members.email

    /**
     * Inserts this member into the database as a [UserReferenceEntity].
     * Assumes that the member data is valid:
     * - [nif] is not null and valid
     * - [email] is not null and valid
     * @param hashedPassword The hashed password to set for the user.
     * @return The created [UserReferenceEntity].
     */
    fun insertUser(hashedPassword: ByteArray) = Database {
        val sub = generateRandomString(16)
        UserReferenceEntity.new(sub) {
            this.memberNumber = this@MemberEntity.memberNumber.toUInt()

            this.nif = this@MemberEntity.nif!!
            this.fullName = this@MemberEntity.fullName
            this.email = this@MemberEntity.email!!

            this.isDisabled = false

            this.groups = listOf("cea_member")

            this.password = hashedPassword
        }
    }

    fun toMember() = Member(memberNumber, status, fullName, nif, email)
}
