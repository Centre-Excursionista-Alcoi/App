package org.centrexcursionistalcoi.app.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.ContentTypeSerializer

object Errors {
    fun serializer(code: Int): KSerializer<out Error>? = when (code) {
        1 -> NotLoggedIn.serializer()
        2 -> NotAnAdmin.serializer()
        3 -> InvalidContentType.serializer()
        4 -> MalformedId.serializer()
        5 -> EntityNotFound.serializer()
        6 -> MissingArgument.serializer()
        7 -> MalformedRequest.serializer()
        8 -> OperationNotSupported.serializer()
        9 -> NothingToUpdate.serializer()
        10 -> MissingFile.serializer()
        11 -> CannotSubmitMemoryUntilMaterialIsReturned.serializer()
        12 -> UserReferenceNotFound.serializer()
        else -> null
    }

    @Serializable
    data object NotLoggedIn : Error(1, "Not logged in", HttpStatusCode.Unauthorized)

    @Serializable
    data object NotAnAdmin : Error(2, "You must be an admin to perform this operation", HttpStatusCode.Forbidden)

    @Serializable
    data class InvalidContentType(
        @Serializable(ContentTypeSerializer::class) val expected: ContentType? = null
    ) : Error(3, "Content-Type must be: $expected", HttpStatusCode.BadRequest)

    @Serializable
    data object MalformedId : Error(4, "Malformed identifier", HttpStatusCode.Forbidden)

    @Serializable
    data class EntityNotFound(
        val entityName: String,
        val id: String
    ) : Error(5, "$entityName#$id not found", HttpStatusCode.NotFound) {
        constructor(entityClass: KClass<*>, id: Any) : this(entityClass.simpleName ?: "Entity", id.toString())
    }

    @Serializable
    data object MissingArgument : Error(6, "Missing argument", HttpStatusCode.BadRequest)

    @Serializable
    data object MalformedRequest : Error(7, "Malformed request", HttpStatusCode.BadRequest)

    @Serializable
    data object OperationNotSupported : Error(8, "Operation not supported", HttpStatusCode.MethodNotAllowed)

    @Serializable
    data object NothingToUpdate : Error(9, "Nothing to update", HttpStatusCode.BadRequest)

    @Serializable
    data object MissingFile : Error(10, "No file uploaded", HttpStatusCode.BadRequest)

    @Serializable
    data object CannotSubmitMemoryUntilMaterialIsReturned : Error(11, "You cannot submit a memory until the material has been returned.", HttpStatusCode.Conflict)

    @Serializable
    data object UserReferenceNotFound : Error(12, "Your user reference was not found.", HttpStatusCode.InternalServerError)

    @Serializable
    data object FEMECVMissingCredentials : Error(13, "Missing \"username\" or \"password\".", HttpStatusCode.BadRequest)

    @Serializable
    data object DeviceIdIsRequired : Error(14, "Device ID is required for this operation.", HttpStatusCode.BadRequest)

    @Serializable
    data object FCMTokenIsRequired : Error(15, "FCM token is required.", HttpStatusCode.BadRequest)

    @Serializable
    data object InvalidTokenId : Error(16, "The provided token ID is invalid.", HttpStatusCode.BadRequest)

}
