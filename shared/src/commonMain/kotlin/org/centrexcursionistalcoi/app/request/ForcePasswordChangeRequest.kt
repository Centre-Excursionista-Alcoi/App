package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable

@Serializable
data class ForcePasswordChangeRequest(val newPassword: String)
