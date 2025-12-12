package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.crypt.encryptedBinary
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.javatime.timestamp

object UserReferences : IdTable<String>(name = "user_references") {
    val sub = text("sub").uniqueIndex().entityId()

    override val id: Column<EntityID<String>> get() = sub

    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val nif = text("nif").uniqueIndex()

    val memberNumber = uinteger("memberNumber").uniqueIndex()

    val fullName = text("fullName")
    val email = text("email").uniqueIndex()
    val groups = array("groups", TextColumnType()).default(emptyList())

    val isDisabled = bool("isDisabled").default(false)

    /**
     * Hashed password using BCrypt.
     * May be empty if the user reference has been created, but the user has not set a password yet.
     */
    val password = encryptedBinary("password", 1024, AES.encryptor)

    val femecvUsername = encryptedVarchar("femecvUsername", 512, AES.encryptor).nullable()
    val femecvPassword = encryptedVarchar("femecvPassword", 512, AES.encryptor).nullable()
    val femecvLastSync = timestamp("femecvLastSync").nullable()
}
