package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.MultiPartData
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingContext
import kotlin.reflect.KClass
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.slf4j.LoggerFactory

/**
 * TODO(#659): this whole file exists only to keep `POST /{base}` working for app installs old enough to still
 * send `multipart/form-data` on entity creation instead of JSON. It has no dependency back into `RoutesBase.kt`
 * beyond [createFromMultipart] itself, on purpose: once every entity has a JSON creator
 * (`provideEntityRoutes`'s `createRequestSerializer`/`jsonCreator`) and the oldest app version this server still
 * needs to serve always sends JSON, delete this file, [createFromMultipart]'s one call site in `RoutesBase.kt`
 * (the multipart branch of `post("/$base")`), and the `creator`/[MultiPartData] parameter (and its KDoc) there.
 */
private val logger = LoggerFactory.getLogger("LegacyMultipartCreate")

/**
 * Creates a new entity from the request's [MultiPartData] via [creator], mapping the exceptions it's documented
 * to throw to the matching [Error] -- see `provideEntityRoutes`'s `creator` parameter for exactly which ones.
 */
internal suspend fun <EE : Any> RoutingContext.createFromMultipart(
    creator: suspend (MultiPartData) -> EE,
    entityKClass: KClass<EE>,
): EE? {
    val multipart = call.receiveMultipart()
    return try {
        creator(multipart)
    } catch (e: NullPointerException) {
        logger.error("Missing argument during entity creation", e)
        respondError(Error.MissingArgument())
        null
    } catch (e: IllegalArgumentException) {
        logger.error("Illegal argument during entity creation", e)
        respondError(Error.MalformedRequest())
        null
    } catch (e: NoSuchElementException) {
        logger.error("Referenced entity not found during entity creation", e)
        respondError(Error.EntityNotFound(entityKClass, "N/A"))
        null
    } catch (e: NumberFormatException) {
        logger.error("Number format exception during entity creation", e)
        respondError(Error.MalformedRequest())
        null
    }
}
