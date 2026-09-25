package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class UserCredentialRecordEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserCredentialRecordEntity>(UserCredentialRecords) {
        /**
         * Deletes the credential [credentialId], but only if it belongs to [userSub]: the id comes from the
         * client, so a credential of any other user is left untouched.
         * @return whether a record was deleted.
         */
        context(_: JdbcTransaction)
        fun deleteIfOwnedBy(credentialId: String, userSub: String): Boolean {
            val record = findById(credentialId) ?: return false
            if (record.readValues[UserCredentialRecords.user].value != userSub) return false
            record.delete()
            return true
        }
    }

    /** Alias for [id] -- the Base64Url-encoded WebAuthn credential ID. */
    val credentialId get() = id

    var user by UserReferenceEntity referencedOn UserCredentialRecords.user
    var attestedCredentialData by UserCredentialRecords.attestedCredentialData
    var signCount by UserCredentialRecords.signCount
}
