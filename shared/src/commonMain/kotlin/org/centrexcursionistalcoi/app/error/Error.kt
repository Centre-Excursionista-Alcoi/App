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
    @SerialName("Unknown")
    class Unknown(val message: String) : Error {
        override val code: Int = 0
        override val description: String = "Internal Server Exception: $message"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("NotLoggedIn")
    class NotLoggedIn() : Error {
        override val code: Int = 1
        override val description: String = "Not logged in"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Unauthorized
    }

    @Serializable
    @SerialName("NotAnAdmin")
    class NotAnAdmin() : Error {
        override val code: Int = 2
        override val description: String = "You must be an admin to perform this operation"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Forbidden
    }

    @Serializable
    @SerialName("InvalidContentType")
    class InvalidContentType(
        @Serializable(ContentTypeSerializer::class) val expected: ContentType? = null
    ) : Error {
        override val code: Int = 2
        override val description: String = "Content-Type must be: $expected"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MalformedId")
    class MalformedId() : Error {
        override val code: Int = 4
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

        override val code: Int = 5
        override val description: String = "$entityName#$id not found"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.NotFound
    }

    @Serializable
    @SerialName("MissingArgument")
    class MissingArgument() : Error {
        override val code: Int = 6
        override val description: String = "Missing argument"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MalformedRequest")
    class MalformedRequest() : Error {
        override val code: Int = 7
        override val description: String = "Malformed request"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("OperationNotSupported")
    class OperationNotSupported() : Error {
        override val code: Int = 8
        override val description: String = "Operation not supported"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.MethodNotAllowed
    }

    @Serializable
    @SerialName("NothingToUpdate")
    class NothingToUpdate() : Error {
        override val code: Int = 9
        override val description: String = "Nothing to update"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MissingFile")
    class MissingFile() : Error {
        override val code: Int = 10
        override val description: String = "No file uploaded"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("CannotSubmitMemoryUntilMaterialIsReturned")
    class CannotSubmitMemoryUntilMaterialIsReturned() : Error {
        override val code: Int = 11
        override val description: String = "You cannot submit a memory until the material has been returned."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("UserReferenceNotFound")
    class UserReferenceNotFound() : Error {
        override val code: Int = 12
        override val description: String = "Your user reference was not found."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
    }

    @Serializable
    @SerialName("FEMECVMissingCredentials")
    class FEMECVMissingCredentials() : Error {
        override val code: Int = 13
        override val description: String = "Missing \"username\" or \"password\"."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("DeviceIdIsRequired")
    class DeviceIdIsRequired() : Error {
        override val code: Int = 14
        override val description: String = "Device ID is required for this operation."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("FCMTokenIsRequired")
    class FCMTokenIsRequired() : Error {
        override val code: Int = 15
        override val description: String = "FCM token is required."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }


    companion object {
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
            else -> null
        }
    }

}
