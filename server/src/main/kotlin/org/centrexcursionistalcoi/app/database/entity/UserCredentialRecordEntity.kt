package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

class UserCredentialRecordEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserCredentialRecordEntity>(UserCredentialRecords)

    /** Alias for [id] -- the Base64Url-encoded WebAuthn credential ID. */
    val credentialId get() = id

    var user by UserReferenceEntity referencedOn UserCredentialRecords.user
    var attestedCredentialData by UserCredentialRecords.attestedCredentialData
    var signCount by UserCredentialRecords.signCount
}
