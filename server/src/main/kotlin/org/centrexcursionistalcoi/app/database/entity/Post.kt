package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.Posts
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class Post(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Post>(Posts)

    var date by Posts.date
    var title by Posts.title
    var content by Posts.content
    var department by Department referencedOn Posts.department
}
