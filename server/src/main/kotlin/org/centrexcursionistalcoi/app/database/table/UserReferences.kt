package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.crypt.encryptedBinary
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object UserReferences : IdTable<String>(name = "user_references") {
    val sub = text("sub").uniqueIndex().entityId()

    override val id: Column<EntityID<String>> get() = sub

    val lastUpdate = timestamp("last_update").defaultExpression(CurrentTimestamp)

    val nif = text("nif").uniqueIndex()

    val memberNumber = uinteger("member").uniqueIndex()

    val fullName = text("full_name")
    val email = text("translate/email")
    val groups = array("groups", TextColumnType()).default(emptyList())

    val isDisabled = bool("is_disabled").default(false)

    /**
     * Hashed password using BCrypt.
     * May be empty if the user reference has been created, but the user has not set a password yet.
     */
    val password = encryptedBinary("password", 1024, AES.encryptor).nullable()

    val femecvUsername = encryptedVarchar("femecv_username", 512, AES.encryptor).nullable()
    val femecvPassword = encryptedVarchar("femecv_password", 512, AES.encryptor).nullable()
    val femecvLastSync = timestamp("femecv_last_sync").nullable()
}
