package storage

object SettingsKeys {
    /**
     * When waiting for an user's email confirmation, this shall be set to the user's ID.
     */
    const val CONFIRMATION_ID = "confirmation-id"

    /**
     * Stores the user's email address after registering, to verify the authentication status.
     */
    const val TEMP_USER_EMAIL = "_email"

    /**
     * Stores the user's password after registering, to verify the authentication status.
     */
    const val TEMP_USER_PASSWORD = "_password"
}
