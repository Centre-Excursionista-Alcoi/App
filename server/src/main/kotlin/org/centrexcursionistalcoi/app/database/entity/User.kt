package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.Users
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<String>): Entity<String>(id) {
    companion object : EntityClass<String, User>(Users)

    val email by Users.id

    var confirmed by Users.confirmed

    var name by Users.name
    var familyName by Users.familyName

    var nif by Users.nif

    var phone by Users.phone

    var salt by Users.salt
    var hash by Users.hash
}
