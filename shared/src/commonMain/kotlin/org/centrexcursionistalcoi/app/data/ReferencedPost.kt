package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Department.Companion.getDepartment
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class ReferencedPost(
    override val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val department: Department?,
    val link: String?,
    val files: List<FileWithContext>,
): ReferencedEntity<Uuid, Post>, ImageFileListContainer {
    companion object {
        fun Post.referenced(departments: List<Department>) = ReferencedPost(
            id = this.id,
            date = this.date,
            title = this.title,
            content = this.content,
            department = this.department?.let { deptId -> departments.getDepartment(deptId) },
            link = this.link,
            files = this.files,
        )
    }

    override fun dereference() = Post(
        id = id,
        date = date,
        title = title,
        content = content,
        department = department?.id,
        link = link,
        files = files,
    )

    override val images: List<Uuid> = files.mapNotNull { it.id }
}
