package org.centrexcursionistalcoi.app.security

object EmailValidation {
    /**
     * Validates the given email address.
     */
    fun validate(email: String?): Boolean {
        email ?: return false
        // Simple regex for email validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        return emailRegex.matches(email)
    }
}
