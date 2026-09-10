package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * Lightweight summary of a user, for the admin user manager's list view. Unlike [UserData], this
 * doesn't hydrate lending user / insurances / departments, so it's cheap to produce for every row
 * of a full user listing.
 */
@Serializable
data class AdminUserSummary(
    val sub: String,
    val memberNumber: UInt,
    val fullName: String,
    val email: String,
    val groups: List<String>,
    val isDisabled: Boolean,
)
