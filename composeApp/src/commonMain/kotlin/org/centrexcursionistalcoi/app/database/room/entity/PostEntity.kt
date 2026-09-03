package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "Posts",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["department"],
        ),
    ],
    indices = [Index(value = ["department"])],
)
data class PostEntity(
    @PrimaryKey
    val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val department: Uuid?,
    val link: String?,
    val files: List<FileWithContext>?,
) {
    fun toPost() = Post(
        id = id,
        date = date,
        title = title,
        content = content,
        department = department,
        link = link,
        files = files.orEmpty(),
    )

    companion object {
        fun Post.toEntity() = PostEntity(
            id = id,
            date = date,
            title = title,
            content = content,
            department = department,
            link = link,
            files = files,
        )
    }
}
