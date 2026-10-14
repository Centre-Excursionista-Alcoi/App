package org.centrexcursionistalcoi.app.integration.femecv

open class FEMECVException(message: String?): Exception(message)

/**
 * FEMECV answered the login with an error about the username or the password, so they are wrong (or were changed).
 * Unlike any other [FEMECVException], which may be FEMECV being down or changing its pages, retrying with the same
 * credentials will fail again.
 */
class FEMECVInvalidCredentialsException(message: String?): FEMECVException(message)
