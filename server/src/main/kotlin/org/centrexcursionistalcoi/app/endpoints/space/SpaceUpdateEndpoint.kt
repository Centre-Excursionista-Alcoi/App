package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceImage
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.SpacesImagesTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.utils.toMonetaryAmount

object SpaceUpdateEndpoint : SecureEndpoint("/spaces", HttpMethod.Patch) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<SpaceD>()
        val id = body.id
        if (id == null) {
            respondFailure(Errors.MissingId)
            return
        }
        val result = ServerDatabase {
            Space.findById(id)
                ?.apply {
                    name = body.name
                    description = body.description

                    capacity = body.capacity?.toUInt()

                    memberPrice = body.memberPrice?.toMonetaryAmount()
                    externalPrice = body.externalPrice?.toMonetaryAmount()

                    setLocation(body.location)
                    setAddress(body.address)

                    // Update images
                    val bodyImages = body.images.orEmpty().map(Base64::decode)
                    val spaceImages = SpaceImage.find { SpacesImagesTable.space eq id }
                    for (image in spaceImages) {
                        val isImageInBody = bodyImages.find { it.contentEquals(image.image) } != null
                        if (!isImageInBody) {
                            image.delete()
                        }
                    }
                    for (image in bodyImages) {
                        val isImageInSpace = spaceImages.find { it.image.contentEquals(image) } != null
                        if (!isImageInSpace) {
                            SpaceImage.new {
                                this.space = this@apply
                                this.image = image
                            }
                        }
                    }
                }
        }
        if (result == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }
        respondSuccess()
    }
}
