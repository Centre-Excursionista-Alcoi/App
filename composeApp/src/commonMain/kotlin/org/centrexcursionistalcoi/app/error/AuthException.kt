package org.centrexcursionistalcoi.app.error

import org.centrexcursionistalcoi.app.server.response.ErrorResponse
import org.centrexcursionistalcoi.app.server.response.Errors

abstract class AuthException(code: Int?, response: String): ServerException(code, response) {
    constructor(error: ErrorResponse): this(error.code, error.message)

    class WrongCredentials: AuthException(Errors.WrongCredentials)

    companion object {
        fun fromCode(code: Int?): ServerException? {
            return when (code) {
                Errors.WrongCredentials.code -> WrongCredentials()
                else -> null
            }
        }
    }
}
