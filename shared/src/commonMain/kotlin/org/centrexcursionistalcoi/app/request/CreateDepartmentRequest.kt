package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext

/**
 * `POST /departments` request body, JSON-only (#659) -- the same shape [UpdateDepartmentRequest] already uses
 * for a patch, with `displayName` required since a department can't exist without one.
 */
@Serializable
data class CreateDepartmentRequest(
    val displayName: String,
    val image: FileWithContext? = null,
)
