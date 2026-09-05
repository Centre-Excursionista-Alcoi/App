package org.centrexcursionistalcoi.app

const val SERVER_PORT = 8080

const val ADMIN_GROUP_NAME = "admin"

/** General (non-department-scoped) role: manages user accounts globally (groups, enable/disable, insurance). */
const val USERS_MANAGER_GROUP_NAME = "users_manager"

/** General (non-department-scoped) role: manages the federation member roster ([org.centrexcursionistalcoi.app.data.Member]). */
const val MEMBERS_MANAGER_GROUP_NAME = "members_manager"
