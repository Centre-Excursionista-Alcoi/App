package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.UsersTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<String>): Entity<String>(id) {
    companion object : EntityClass<String, User>(UsersTable)

    var confirmed by UsersTable.confirmed
    var isAdmin by UsersTable.isAdmin

    var name by UsersTable.name
    var familyName by UsersTable.familyName

    var nif by UsersTable.nif

    var phone by UsersTable.phone

    var salt by UsersTable.salt
    var hash by UsersTable.hash
}
