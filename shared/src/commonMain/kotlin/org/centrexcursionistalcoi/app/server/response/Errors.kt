package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

object Errors {
    @Serializable
    object InvalidEmail : ErrorResponse(1, "The email provided is not valid")

    @Serializable
    object UnsafePassword : ErrorResponse(2, "The password provided is not safe enough")

    @Serializable
    object InvalidCredentials : ErrorResponse(3, "The credentials given are not valid")

    @Serializable
    object InvalidRequest : ErrorResponse(4, "The request made contains some invalid data")

    @Serializable
    object UserAlreadyExists : ErrorResponse(5, "User already exists")
}
