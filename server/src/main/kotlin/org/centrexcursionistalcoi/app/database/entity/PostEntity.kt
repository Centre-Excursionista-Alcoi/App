package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.PostEntity.Companion.forSession
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.jdbc.insert

class PostEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<Post, Uuid>, EntityPatcher<UpdatePostRequest> {
    companion object : UuidEntityClass<PostEntity>(Posts) {
        context(_: JdbcTransaction)
        fun forSession(session: UserSession?) = when {
            session == null -> {
                // Not logged in, only show public posts (without department)
                find { Posts.department eq null }
            }
            session.isAdmin() -> {
                // If admin, show all posts
                all()
            }
            else -> {
                // Logged in, show public posts, and posts for the user's department
                val userDepartments = transaction {
                    DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true).map { it.department.id.value }
                }
                find {
                    (Posts.department eq null) or (Posts.department inList userDepartments)
                }
            }
        }
    }

    /**
     * Whether this single post is visible to [session] -- must stay in sync with [forSession], which is the
     * same rule applied at the list level. Evaluated directly against this entity's own department (one
     * department lookup for the caller, not a query over every post), so this is safe to call per single-item GET.
     */
    context(_: JdbcTransaction)
    fun isVisibleTo(session: UserSession?): Boolean {
        val postDepartmentId = department?.id?.value ?: return true
        return session != null && (
            session.isAdmin() ||
                DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true).any { it.department.id.value == postDepartmentId }
            )
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    val date by Posts.date
    override var lastUpdate by Posts.lastUpdate

    var title by Posts.title
    var content by Posts.content
    var department by DepartmentEntity optionalReferencedOn Posts.department
    var link by Posts.link

    val files by FileEntity via PostFiles

    @OptIn(ExperimentalTime::class)
    context(_: JdbcTransaction)
    override fun toData(): Post = Post(
        id = id.value,
        date = date,
        title = title,
        content = content,
        department = department?.id?.value,
        link = link,
        files = files.map { it.toData() },
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdatePostRequest) {
        request.title?.let { title = it }
        request.content?.let { content = it }
        request.department?.let {
            department = DepartmentEntity.findById(it)
        }
        request.link?.let { link = it.takeUnless { value -> value.isBlank() } }
        val ownedFileIds = files.map { it.id.value }
        request.files?.forEach { fileWithContext ->
            val fileEntity = FileEntity.updateOrCreate(fileWithContext, ownedFileIds) { fileEntity ->
                PostFiles.deleteWhere { (PostFiles.post eq this@PostEntity.id) and (PostFiles.file eq fileEntity.id) }
            }
            if (fileEntity != null) {
                // A new file was created (fileWithContext had bytes) -- link it to this post.
                PostFiles.insert {
                    it[post] = this@PostEntity.id
                    it[file] = fileEntity.id
                }
            }
        }
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    override fun delete() {
        val files = files.toList()
        super.delete() // post_files cascades
        FileEntity.deleteOwnedFiles(files)
    }
}
