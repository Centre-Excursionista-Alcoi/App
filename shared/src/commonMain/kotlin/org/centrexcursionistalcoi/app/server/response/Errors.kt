package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

object Errors {
    @Serializable
    data object InvalidEmail : ErrorResponse(1, "The email provided is not valid")

    @Serializable
    data object UnsafePassword : ErrorResponse(2, "The password provided is not safe enough")

    @Serializable
    data object InvalidCredentials : ErrorResponse(3, "The credentials given are not valid")

    @Serializable
    data object InvalidRequest : ErrorResponse(4, "The request made contains some invalid data")

    @Serializable
    data object UserAlreadyExists : ErrorResponse(5, "User already exists")

    @Serializable
    data object WrongCredentials : ErrorResponse(6, "Email-password combination is not correct")

    @Serializable
    data object NotLoggedIn : ErrorResponse(7, "Not logged in")

    @Serializable
    data object UserNotFound : ErrorResponse(8, "User not found")

    @Serializable
    data object Forbidden : ErrorResponse(9, "No permission", 403 /* Forbidden */)

    @Serializable
    data object MissingId : ErrorResponse(10, "Missing id")

    @Serializable
    data object ObjectNotFound : ErrorResponse(11, "Object not found")

    @Serializable
    data object MissingReferenceId : ErrorResponse(12, "Missing reference id")

    @Serializable
    data object ReferenceNotFound : ErrorResponse(13, "Reference not found")

    @Serializable
    data object UserNotConfirmed : ErrorResponse(14, "User not confirmed")

    @Serializable
    data object ObjectNotAvailable : ErrorResponse(15, "Object not available")

    @Serializable
    data object BookingNotConfirmed : ErrorResponse(16, "Booking not confirmed", 412 /* Conflict */)

    @Serializable
    data object BookingNotTaken : ErrorResponse(17, "Booking not taken", 409 /* Conflict */)

    @Serializable
    data object BookingAlreadyReturned : ErrorResponse(18, "Booking already returned", 409 /* Conflict */)

    @Serializable
    data object SpaceWithoutKeys : ErrorResponse(19, "The space doesn't have any defined key", 406 /* Not Acceptable */)

    @Serializable
    data object KeyNotSpecified : ErrorResponse(20, "The space requires a key to be given")

    @Serializable
    data object BookingTaken : ErrorResponse(21, "Booking taken", 409 /* Conflict */)
}
