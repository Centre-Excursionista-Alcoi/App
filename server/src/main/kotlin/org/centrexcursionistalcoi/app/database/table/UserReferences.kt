package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.crypt.encryptedVarchar

object UserReferences : IdTable<String>(name = "user_references") {
    val sub = text("sub").uniqueIndex().entityId()

    override val id: Column<EntityID<String>> get() = sub

    val username = text("username")
    val email = text("email")
    val groups = array("groups", TextColumnType())

    val femecvUsername = encryptedVarchar("femecv_username", 512, AES.encryptor).nullable()
    val femecvPassword = encryptedVarchar("femecv_password", 512, AES.encryptor).nullable()
}
