package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext

/**
 * `POST /departments` request body: the same shape [UpdateDepartmentRequest] uses for a patch, with
 * `displayName` required since a department can't exist without one.
 */
@Serializable
data class CreateDepartmentRequest(
    val displayName: String,
    val image: FileWithContext? = null,
) : RequestWithFiles<CreateDepartmentRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext): CreateDepartmentRequest = copy(image = image?.let(transform))
}
