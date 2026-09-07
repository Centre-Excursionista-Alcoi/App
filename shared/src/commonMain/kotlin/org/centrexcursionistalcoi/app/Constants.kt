package org.centrexcursionistalcoi.app

const val SERVER_PORT = 8080

const val ADMIN_GROUP_NAME = "admin"

/**
 * General (non-department-scoped) role: read-only visibility into the full user list (`GET /users`), regardless
 * of department membership. Does NOT grant any account-mutating capability -- promoting a user to [ADMIN_GROUP_NAME]
 * (or any other group change) stays admin-only, since a non-admin role must never be able to grant itself admin.
 */
const val USERS_MANAGER_GROUP_NAME = "users_manager"

/** General (non-department-scoped) role: manages the federation member roster ([org.centrexcursionistalcoi.app.data.Member]). */
const val MEMBERS_MANAGER_GROUP_NAME = "members_manager"
