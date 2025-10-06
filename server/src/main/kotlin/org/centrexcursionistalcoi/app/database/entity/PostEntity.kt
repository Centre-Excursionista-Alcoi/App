package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.table.Posts
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class PostEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Post, Uuid> {
    companion object : UUIDEntityClass<PostEntity>(Posts)

    var date by Posts.date
    var title by Posts.title
    var content by Posts.content
    var onlyForMembers by Posts.onlyForMembers
    var department by Posts.department

    @OptIn(ExperimentalTime::class)
    context(_: JdbcTransaction)
    override fun toData(): Post = Post(
        id = id.value.toKotlinUuid(),
        date = date.toKotlinInstant(),
        title = title,
        content = content,
        onlyForMembers = onlyForMembers,
        departmentId = department.value,
    )
}
