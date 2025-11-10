package org.centrexcursionistalcoi.app.security

import at.favre.lib.crypto.bcrypt.BCrypt

object Passwords {
    /**
     * Hashes a password using BCrypt.
     * @param password The password to hash.
     * @param costFactor The cost factor (log rounds) for the hashing algorithm. Default is 12.
     * @return The hashed password as a ByteArray.
     * @throws IllegalArgumentException if the password length exceeds 72 characters.
     */
    fun hash(password: CharArray, costFactor: Int = 12): ByteArray {
        require(password.size <= 72) { "BCrypt supports a maximum password length of 72 characters." }
        return BCrypt.withDefaults().hash(costFactor, password)
    }

    /**
     * Verifies a password against a given hash using BCrypt.
     * @param password The password to verify.
     * @param hash The hashed password to verify against.
     * @return `true` if the password matches the hash, `false` otherwise.
     */
    fun verify(password: CharArray, hash: ByteArray) = BCrypt.verifyer().verify(password, hash).verified

    /**
     * Checks if a password is considered safe based on defined criteria.
     * A safe password must contain at least one lowercase letter, one uppercase letter,
     * one number, and be at least 8 characters long.
     * @param password The password to check.
     * @return `true` if the password is safe, `false` otherwise.
     */
    fun isSafe(password: CharArray): Boolean {
        val hasLowerCase = password.find { it.isLowerCase() } != null
        val hasUpperCase = password.find { it.isUpperCase() } != null
        val hasNumber = password.find { it.isDigit() } != null
        val hasMinLength = password.size >= 8
        return hasLowerCase && hasUpperCase && hasNumber && hasMinLength
    }
}
