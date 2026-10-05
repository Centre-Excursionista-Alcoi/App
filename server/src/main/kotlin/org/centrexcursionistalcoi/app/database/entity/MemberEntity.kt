package org.centrexcursionistalcoi.app.database.entity

import java.util.Locale
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
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
     * @param hashedPassword The hashed password to set for the user, or `null` if they sign in with passkeys only.
     * @param sub The id of the user, if it has already been chosen (e.g. for the user handle of their passkey).
     * @param language The language of the user, if known (the one of the request that registers them).
     * @return The created [UserReferenceEntity].
     */
    fun insertUser(hashedPassword: ByteArray?, sub: String = generateUserSub(), language: Locale? = null) = Database {
        UserReferenceEntity.new(sub) {
            this.memberNumber = this@MemberEntity.memberNumber

            this.nif = this@MemberEntity.nif!!
            this.fullName = this@MemberEntity.fullName
            this.email = this@MemberEntity.email!!

            this.isDisabled = false

            this.groups = listOf("cea_member")

            this.password = hashedPassword ?: ByteArray(0)
        }.also { user ->
            // After the user, which the preferences reference
            language?.let { UserPreferenceStore.setIfMissing(user.sub.value, UserPreferenceKey.Language, it) }
        }
    }

    fun toMember() = Member(memberNumber, status, fullName, nif, email)
}

/** A new, random id for a user. */
fun generateUserSub(): String = generateRandomString(16)
