package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity

fun Route.departmentsRoutes() {
    provideEntityRoutes(
        base = "departments",
        entityClass = DepartmentEntity,
        idTypeConverter = { it.toIntOrNull() },
        creator = { formParameters ->
            var displayName: String? = null
            var contentType: ContentType? = null
            var originalFileName: String? = null
            val imageDataStream = ByteArrayOutputStream()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "displayName") {
                            displayName = partData.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (partData.name == "image") {
                            contentType = partData.contentType
                            originalFileName = partData.originalFileName
                            partData.provider().copyTo(imageDataStream.asByteWriteChannel())
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                throw NullPointerException("Missing displayName")
            }

            Database {
                val imageFile = if (imageDataStream.size() > 0) {
                    FileEntity.new {
                        name = originalFileName ?: "unknown"
                        type = contentType?.toString() ?: "application/octet-stream"
                        data = imageDataStream.toByteArray()
                    }
                } else null

                DepartmentEntity.new {
                    this.displayName = displayName
                    this.imageFile = imageFile?.id
                }
            }
        }
    )
}
