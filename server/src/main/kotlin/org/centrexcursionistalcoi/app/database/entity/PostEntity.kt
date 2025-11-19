package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class PostEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Post, Uuid>, EntityPatcher<UpdatePostRequest> {
    companion object : UUIDEntityClass<PostEntity>(Posts)

    val date by Posts.date
    var lastUpdate by Posts.lastUpdate

    var title by Posts.title
    var content by Posts.content
    var department by DepartmentEntity optionalReferencedOn Posts.department

    @OptIn(ExperimentalTime::class)
    context(_: JdbcTransaction)
    override fun toData(): Post = Post(
        id = id.value.toKotlinUuid(),
        date = date.toKotlinInstant(),
        title = title,
        content = content,
        department = department?.id?.value?.toKotlinUuid(),
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdatePostRequest) {
        request.title?.let { title = it }
        request.content?.let { content = it }
        request.department?.let {
            department = DepartmentEntity.findById(it.toJavaUuid())
        }
        lastUpdate = java.time.Instant.now()
    }
}
