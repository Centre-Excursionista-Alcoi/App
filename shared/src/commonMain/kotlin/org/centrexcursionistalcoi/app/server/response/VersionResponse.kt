package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val latest: String?,
    val urls: DownloadUrls
): SuccessResponse() {
    @Serializable
    data class DownloadUrls(
        val apk: String?,
        val exe: String?,
        val dmg: String?,
        val deb: String?
    )
}
