package org.centrexcursionistalcoi.app.database.entity

import java.time.Instant
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory

class PostEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Post, Uuid>, EntityPatcher<UpdatePostRequest> {
    companion object : UUIDEntityClass<PostEntity>(Posts)

    private val logger = LoggerFactory.getLogger(this::class.java)

    val date by Posts.date
    var lastUpdate by Posts.lastUpdate

    var title by Posts.title
    var content by Posts.content
    var department by DepartmentEntity optionalReferencedOn Posts.department
    var link by Posts.link

    val files by FileEntity via PostFiles

    @OptIn(ExperimentalTime::class)
    context(_: JdbcTransaction)
    override fun toData(): Post = Post(
        id = id.value.toKotlinUuid(),
        date = date.toKotlinInstant(),
        title = title,
        content = content,
        department = department?.id?.value?.toKotlinUuid(),
        link = link,
        files = files.map { it.id.value.toKotlinUuid() },
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdatePostRequest) {
        request.title?.let { title = it }
        request.content?.let { content = it }
        request.department?.let {
            department = DepartmentEntity.findById(it.toJavaUuid())
        }
        request.link?.let { link = it }
        request.files?.forEach { fileWithContext ->
            if (fileWithContext.isEmpty()) {
                // No bytes given, remove existing file
                val fileId = fileWithContext.id?.toJavaUuid()
                if (fileId != null) {
                    // Remove file
                    FileEntity.findById(fileId)?.let {
                        logger.info("Removing file $fileId from post $id")
                        PostFiles.deleteWhere { (PostFiles.post eq id) and (PostFiles.file eq fileId) }
                        it.delete()
                    } ?: run {
                        logger.warn("Asked to remove file $fileId from post $id, but file does not exist, ignoring")
                    }
                } else {
                    // Asked to remove, but no id given, ignore
                    logger.warn("Asked to remove file from post $id, but no id given, ignoring")
                }
            } else {
                // Create a new file
                FileEntity.from(fileWithContext)
            }
        }
        lastUpdate = Instant.now()
    }
}
