package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.security.Passwords
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object Users : IdTable<String>("users") {
    // Emails have a max length of 320 characters
    override val id: Column<EntityID<String>> = varchar("email", 320).entityId()

    val confirmed = bool("confirmed").default(false)

    val name = varchar("name", 128)
    val familyName = varchar("family_name", 256)

    // NIFs have at most 9 chars (87654321X / X12345467X)
    val nif = varchar("nif", 9)

    val phone = varchar("phone", 20)

    val salt = binary("salt", Passwords.SALT_LENGTH)
    val hash = binary("hash", Passwords.HASH_LENGTH)
}
