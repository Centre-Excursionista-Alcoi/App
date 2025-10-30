package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.utils.toUUID

fun Route.spacesRoutes() {
    provideEntityRoutes(
        base = "spaces",
        entityClass = SpaceEntity,
        idTypeConverter = { it.toUUID() },
        creator = { formParameters ->
            var name: String? = null
            var description: String? = null
            var price: Double? = null
            var priceDurationSeconds: Long? = null
            var capacity: Int? = null

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> when(partData.name) {
                        "name" -> name = partData.value
                        "description" -> description = partData.value
                        "price" -> price = partData.value.toDoubleOrNull()
                        "priceDurationSeconds" -> priceDurationSeconds = partData.value.toLongOrNull()
                        "capacity" -> capacity = partData.value.toIntOrNull()
                    }
                    else -> { /* nothing */ }
                }
            }

            if (name == null) {
                throw NullPointerException("Missing name")
            }

            Database {
                SpaceEntity.new {
                    this.name = name
                    this.description = description
                    this.price = price?.toBigDecimal()
                    this.priceDuration = priceDurationSeconds?.seconds?.toJavaDuration()
                    this.capacity = capacity
                }
            }
        },
        updater = UpdateSpaceRequest.serializer()
    )
}
