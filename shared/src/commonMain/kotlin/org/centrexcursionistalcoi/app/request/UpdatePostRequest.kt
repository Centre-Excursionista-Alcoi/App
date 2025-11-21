package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post

@Serializable
data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val department: Uuid? = null,
    val link: String? = null,
    val files: List<FileWithContext>? = null,
): UpdateEntityRequest<Uuid, Post> {
    override fun isEmpty(): Boolean {
        return title.isNullOrEmpty() && content.isNullOrEmpty() && department == null && link.isNullOrBlank() && files.isNullOrEmpty()
    }
}
