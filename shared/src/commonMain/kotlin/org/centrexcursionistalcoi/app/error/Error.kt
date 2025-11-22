package org.centrexcursionistalcoi.app.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KClass
import kotlin.uuid.Uuid
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
    class MissingArgument(val argumentName: String? = null) : Error {
        override val code: Int = ERROR_MISSING_ARGUMENT
        override val description: String = if (argumentName == null) "Missing argument" else "Missing argument: $argumentName"

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
        override val description: String = "Memory not given. Must set at least \"text\""

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
    @SerialName("InvalidArgument")
    class InvalidArgument(
        val argument: String? = null,
        val message: String? = null,
    ) : Error {
        override val code: Int = ERROR_INVALID_ARGUMENT
        override val description: String = message ?: "Invalid argument${if (argument != null) ": $argument" else ""}."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
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

    @Serializable
    @SerialName("EndDateCannotBeBeforeStart")
    class EndDateCannotBeBeforeStart(): Error {
        override val code: Int = ERROR_END_DATE_CANNOT_BE_BEFORE_START
        override val description: String = "End date cannot be before start date."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("DateMustBeInFuture")
    class DateMustBeInFuture(): Error {
        override val code: Int = ERROR_DATE_MUST_BE_IN_FUTURE
        override val description: String = "The date must be in the future."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("ListCannotBeEmpty")
    class ListCannotBeEmpty(val argName: String? = null): Error {
        override val code: Int = ERROR_LIST_CANNOT_BE_EMPTY
        override val description: String = "\"${argName ?: "List"}\" cannot be empty."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("MemoryNotSubmitted")
    class MemoryNotSubmitted(): Error {
        override val code: Int = ERROR_MEMORY_NOT_SUBMITTED
        override val description: String = "You have a lending with a pending memory."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.PreconditionFailed
    }

    @Serializable
    @SerialName("LendingConflict")
    class LendingConflict(): Error {
        override val code: Int = ERROR_LENDING_CONFLICT
        override val description: String = "There are conflicts with your lending request: there's another lending overlapping with the same items."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("LendingNotTaken")
    class LendingNotTaken(val lendingId: Uuid? = null): Error {
        override val code: Int = ERROR_LENDING_NOT_TAKEN
        override val description: String = "Lending with id $lendingId has not been taken yet."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.PreconditionFailed
    }

    @Serializable
    @SerialName("InvalidItemInReturnedItems")
    class InvalidItemInReturnedItems(): Error {
        override val code: Int = ERROR_INVALID_ITEM_IN_RETURNED_ITEMS
        override val description: String = "One or more items in the returned items list are invalid."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("PasswordNotSafeEnough")
    class PasswordNotSafeEnough(): Error {
        override val code: Int = ERROR_PASSWORD_NOT_SAFE_ENOUGH
        override val description: String = "Password is not safe enough. It must be at least 8 characters long and contain at least one lowercase letter, one uppercase letter, and one number."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
    }

    @Serializable
    @SerialName("NIFNotRegistered")
    class NIFNotRegistered(): Error {
        override val code: Int = ERROR_NIF_NOT_REGISTERED
        override val description: String = "There is not any user registered with the given NIF."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.NotFound
    }

    @Serializable
    @SerialName("IncorrectPasswordOrNIF")
    class IncorrectPasswordOrNIF(): Error {
        override val code: Int = ERROR_INCORRECT_PASSWORD_OR_NIF
        override val description: String = "The NIF or password is incorrect."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Unauthorized
    }

    @Serializable
    @SerialName("PasswordNotSet")
    class PasswordNotSet(): Error {
        override val code: Int = ERROR_PASSWORD_NOT_SET
        override val description: String = "The user has not set a password yet."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.PreconditionFailed
    }

    @Serializable
    @SerialName("UserAlreadyRegisteredForLending")
    class UserAlreadyRegisteredForLending(): Error {
        override val code: Int = ERROR_USER_ALREADY_REGISTERED_FOR_LENDING
        override val description: String = "The user is already registered for lending."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("LendingAlreadyPickedUp")
    class LendingAlreadyPickedUp(): Error {
        override val code: Int = ERROR_LENDING_ALREADY_PICKED_UP
        override val description: String = "The lending has already been picked up and cannot be cancelled."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("UserNotSignedUpForLending")
    class UserNotSignedUpForLending(): Error {
        override val code: Int = ERROR_USER_NOT_SIGNED_UP_FOR_LENDING
        override val description: String = "The user is not signed up for lendings."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Forbidden
    }

    @Serializable
    @SerialName("LendingNotConfirmed")
    class LendingNotConfirmed(): Error {
        override val code: Int = ERROR_LENDING_NOT_CONFIRMED
        override val description: String = "The lending is not confirmed"

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.PreconditionFailed
    }

    @Serializable
    @SerialName("UserAlreadyRegistered")
    class UserAlreadyRegistered(): Error {
        override val code: Int = ERROR_USER_ALREADY_REGISTERED
        override val description: String = "The user is already registered: already has a password."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Conflict
    }

    @Serializable
    @SerialName("UserDoesNotHaveInsurance")
    class UserDoesNotHaveInsurance(): Error {
        override val code: Int = ERROR_USER_DOES_NOT_HAVE_INSURANCE
        override val description: String = "The user doesn't have a valid and active insurance."

        @Serializable(HttpStatusCodeSerializer::class)
        override val statusCode: HttpStatusCode = HttpStatusCode.Forbidden
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
        const val ERROR_INVALID_ARGUMENT = 18
        const val ERROR_SERIALIZATION_ERROR = 19
        const val ERROR_ENTITY_DELETE_REFERENCES_EXIST = 20
        const val ERROR_END_DATE_CANNOT_BE_BEFORE_START = 21
        const val ERROR_DATE_MUST_BE_IN_FUTURE = 22
        const val ERROR_LIST_CANNOT_BE_EMPTY = 23
        const val ERROR_MEMORY_NOT_SUBMITTED = 24
        const val ERROR_LENDING_CONFLICT = 25
        const val ERROR_LENDING_NOT_TAKEN = 26
        const val ERROR_INVALID_ITEM_IN_RETURNED_ITEMS = 27
        const val ERROR_PASSWORD_NOT_SAFE_ENOUGH = 28
        const val ERROR_NIF_NOT_REGISTERED = 29
        const val ERROR_INCORRECT_PASSWORD_OR_NIF = 30
        const val ERROR_PASSWORD_NOT_SET = 31
        const val ERROR_USER_ALREADY_REGISTERED_FOR_LENDING = 32
        const val ERROR_LENDING_ALREADY_PICKED_UP = 33
        const val ERROR_USER_NOT_SIGNED_UP_FOR_LENDING = 34
        const val ERROR_LENDING_NOT_CONFIRMED = 35
        const val ERROR_USER_ALREADY_REGISTERED = 36
        const val ERROR_USER_DOES_NOT_HAVE_INSURANCE = 37

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
            18 -> InvalidArgument.serializer()
            19 -> SerializationError.serializer()
            20 -> EntityDeleteReferencesExist.serializer()
            ERROR_END_DATE_CANNOT_BE_BEFORE_START -> EndDateCannotBeBeforeStart.serializer()
            ERROR_DATE_MUST_BE_IN_FUTURE -> DateMustBeInFuture.serializer()
            ERROR_LIST_CANNOT_BE_EMPTY -> ListCannotBeEmpty.serializer()
            ERROR_MEMORY_NOT_SUBMITTED -> MemoryNotSubmitted.serializer()
            ERROR_LENDING_CONFLICT -> LendingConflict.serializer()
            ERROR_LENDING_NOT_TAKEN -> LendingNotTaken.serializer()
            ERROR_INVALID_ITEM_IN_RETURNED_ITEMS -> InvalidItemInReturnedItems.serializer()
            ERROR_PASSWORD_NOT_SAFE_ENOUGH -> PasswordNotSafeEnough.serializer()
            ERROR_NIF_NOT_REGISTERED -> NIFNotRegistered.serializer()
            ERROR_INCORRECT_PASSWORD_OR_NIF -> IncorrectPasswordOrNIF.serializer()
            ERROR_PASSWORD_NOT_SET -> PasswordNotSet.serializer()
            ERROR_USER_ALREADY_REGISTERED_FOR_LENDING -> UserAlreadyRegisteredForLending.serializer()
            ERROR_LENDING_ALREADY_PICKED_UP -> LendingAlreadyPickedUp.serializer()
            ERROR_USER_NOT_SIGNED_UP_FOR_LENDING -> UserNotSignedUpForLending.serializer()
            ERROR_LENDING_NOT_CONFIRMED -> LendingNotConfirmed.serializer()
            ERROR_USER_ALREADY_REGISTERED -> UserAlreadyRegistered.serializer()
            ERROR_USER_DOES_NOT_HAVE_INSURANCE -> UserDoesNotHaveInsurance.serializer()
            else -> null
        }
    }

}
