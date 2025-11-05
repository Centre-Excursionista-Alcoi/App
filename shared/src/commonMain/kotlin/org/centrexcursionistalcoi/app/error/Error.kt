package org.centrexcursionistalcoi.app.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.serializer.ContentTypeSerializer
import org.centrexcursionistalcoi.app.serializer.HttpStatusCodeSerializer

@Serializable
sealed interface Error {
    val code: Int
    val description: String?

    @Serializable(HttpStatusCodeSerializer::class)
    val statusCode: HttpStatusCode

    fun toThrowable(): ServerException {
        return ServerException(
            message = description,
            responseStatusCode = statusCode.value,
            responseBody = null,
            errorCode = code,
        )
    }


    @Serializable
    @SerialName("Exception")
    class Exception(val message: String, val classType: String, val cause: Exception?) : Error {
        constructor(throwable: Throwable) : this(
            throwable.message ?: "No message",
            throwable::class.simpleName ?: "Unknown",
            throwable.cause?.let { Exception(it) },
        )

        override val code: Int = -1
        override val description: String = "Unhandled Exception ($classType): $message"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("Unknown")
    class Unknown(val message: String) : Error {
        override val code: Int = ERROR_UNKNOWN
        override val description: String = "Internal Server Exception: $message"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("NotLoggedIn")
    class NotLoggedIn() : Error {
        override val code: Int = ERROR_NOT_LOGGED_IN
        override val description: String = "Not logged in"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Unauthorized
    }

    @Serializable
    @SerialName("NotAnAdmin")
    class NotAnAdmin() : Error {
        override val code: Int = ERROR_NOT_AN_ADMIN
        override val description: String = "You must be an admin to perform this operation"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Forbidden
    }

    @Serializable
    @SerialName("InvalidContentType")
    class InvalidContentType(
        @Serializable(ContentTypeSerializer::class) val expected: ContentType? = null,
        @Serializable(ContentTypeSerializer::class) val actual: ContentType? = null,
    ) : Error {
        override val code: Int = ERROR_INVALID_CONTENT_TYPE
        override val description: String = "Content-Type must be: $expected"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MalformedId")
    class MalformedId() : Error {
        override val code: Int = ERROR_MALFORMED_ID
        override val description: String = "Malformed identifier"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Forbidden
    }

    @Serializable
    @SerialName("EntityNotFound")
    class EntityNotFound(
        val entityName: String,
        val id: String
    ) : Error {
        constructor(entityClass: KClass<*>, id: Any) : this(entityClass.simpleName ?: "Entity", id.toString())

        override val code: Int = ERROR_ENTITY_NOT_FOUND
        override val description: String = "$entityName#$id not found"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.NotFound
    }

    @Serializable
    @SerialName("MissingArgument")
    class MissingArgument() : Error {
        override val code: Int = ERROR_MISSING_ARGUMENT
        override val description: String = "Missing argument"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MalformedRequest")
    class MalformedRequest() : Error {
        override val code: Int = ERROR_MALFORMED_REQUEST
        override val description: String = "Malformed request"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("OperationNotSupported")
    class OperationNotSupported() : Error {
        override val code: Int = ERROR_OPERATION_NOT_SUPPORTED
        override val description: String = "Operation not supported"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.MethodNotAllowed
    }

    @Serializable
    @SerialName("NothingToUpdate")
    class NothingToUpdate() : Error {
        override val code: Int = ERROR_NOTHING_TO_UPDATE
        override val description: String = "Nothing to update"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MissingFile")
    class MissingFile() : Error {
        override val code: Int = ERROR_MISSING_FILE
        override val description: String = "No file uploaded"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("CannotSubmitMemoryUntilMaterialIsReturned")
    class CannotSubmitMemoryUntilMaterialIsReturned() : Error {
        override val code: Int = ERROR_CANNOT_SUBMIT_MEMORY_UNTIL_MATERIAL_IS_RETURNED
        override val description: String = "You cannot submit a memory until the material has been returned."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("UserReferenceNotFound")
    class UserReferenceNotFound() : Error {
        override val code: Int = ERROR_USER_REFERENCE_NOT_FOUND
        override val description: String = "Your user reference was not found."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("FEMECVMissingCredentials")
    class FEMECVMissingCredentials() : Error {
        override val code: Int = ERROR_FEMECV_MISSING_CREDENTIALS
        override val description: String = "Missing \"username\" or \"password\"."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("DeviceIdIsRequired")
    class DeviceIdIsRequired() : Error {
        override val code: Int = ERROR_DEVICE_ID_IS_REQUIRED
        override val description: String = "Device ID is required for this operation."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("FCMTokenIsRequired")
    class FCMTokenIsRequired() : Error {
        override val code: Int = ERROR_FCM_TOKEN_IS_REQUIRED
        override val description: String = "FCM token is required."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MemoryNotGiven")
    class MemoryNotGiven() : Error {
        override val code: Int = ERROR_MEMORY_NOT_GIVEN
        override val description: String = "Memory not given. Set either \"file\" or \"text\""

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("UserNotFound")
    class UserNotFound() : Error {
        override val code: Int = ERROR_USER_NOT_FOUND
        override val description: String = "An user was not found with the given sub."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.NotFound
    }

    @Serializable
    @SerialName("AuthentikNotConfigured")
    class AuthentikNotConfigured() : Error {
        override val code: Int = ERROR_AUTHENTIK_NOT_CONFIGURED
        override val description: String = "Authentik is not configured on the server."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.ServiceUnavailable
    }

    @Serializable
    @SerialName("SerializationError")
    class SerializationError(val message: String?, val content: String?) : Error {
        override val code: Int = ERROR_SERIALIZATION_ERROR
        override val description: String = "There was a serialization error.\n\tMessage: $message\n\tContent: $content"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("EntityDeleteReferencesExist")
    class EntityDeleteReferencesExist(): Error {
        override val code: Int = ERROR_ENTITY_DELETE_REFERENCES_EXIST
        override val description: String = "Cannot delete entity because other entities reference it."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }


    companion object {
        const val ERROR_UNKNOWN = 0
        const val ERROR_NOT_LOGGED_IN = 1
        const val ERROR_NOT_AN_ADMIN = 2
        const val ERROR_INVALID_CONTENT_TYPE = 3
        const val ERROR_MALFORMED_ID = 4
        const val ERROR_ENTITY_NOT_FOUND = 5
        const val ERROR_MISSING_ARGUMENT = 6
        const val ERROR_MALFORMED_REQUEST = 7
        const val ERROR_OPERATION_NOT_SUPPORTED = 8
        const val ERROR_NOTHING_TO_UPDATE = 9
        const val ERROR_MISSING_FILE = 10
        const val ERROR_CANNOT_SUBMIT_MEMORY_UNTIL_MATERIAL_IS_RETURNED = 11
        const val ERROR_USER_REFERENCE_NOT_FOUND = 12
        const val ERROR_FEMECV_MISSING_CREDENTIALS = 13
        const val ERROR_DEVICE_ID_IS_REQUIRED = 14
        const val ERROR_FCM_TOKEN_IS_REQUIRED = 15
        const val ERROR_MEMORY_NOT_GIVEN = 16
        const val ERROR_USER_NOT_FOUND = 17
        const val ERROR_AUTHENTIK_NOT_CONFIGURED = 18
        const val ERROR_SERIALIZATION_ERROR = 19
        const val ERROR_ENTITY_DELETE_REFERENCES_EXIST = 20

        fun serializer(code: Int): KSerializer<out Error>? = when (code) {
            0 -> Unknown.serializer()
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
            13 -> FEMECVMissingCredentials.serializer()
            14 -> DeviceIdIsRequired.serializer()
            15 -> FCMTokenIsRequired.serializer()
            16 -> MemoryNotGiven.serializer()
            17 -> UserNotFound.serializer()
            18 -> AuthentikNotConfigured.serializer()
            19 -> SerializationError.serializer()
            20 -> EntityDeleteReferencesExist.serializer()
            else -> null
        }
    }

}
