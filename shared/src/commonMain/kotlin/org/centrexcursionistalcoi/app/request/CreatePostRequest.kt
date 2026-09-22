package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext

/**
 * `POST /posts` request body, JSON-only (#659) -- the same shape [UpdatePostRequest] already uses for a patch,
 * with `title`/`content` required since a post can't exist without them.
 */
@Serializable
data class CreatePostRequest(
    val title: String,
    val content: String,
    val department: Uuid? = null,
    val link: String? = null,
    val files: List<FileWithContext> = emptyList(),
)
