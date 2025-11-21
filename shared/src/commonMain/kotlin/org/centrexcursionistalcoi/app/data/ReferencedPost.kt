package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Department.Companion.getDepartment

@Serializable
data class ReferencedPost(
    override val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val department: Department?,
    val link: String?,
    val files: List<Uuid>,

    override val referencedEntity: Post
): ReferencedEntity<Uuid, Post>() {
    companion object {
        fun Post.referenced(departments: List<Department>) = ReferencedPost(
            id = this.id,
            date = this.date,
            title = this.title,
            content = this.content,
            department = this.department?.let { deptId -> departments.getDepartment(deptId) },
            link = this.link,
            files = this.files,
            referencedEntity = this
        )
    }
}
