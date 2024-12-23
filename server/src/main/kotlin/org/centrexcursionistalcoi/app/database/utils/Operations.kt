package org.centrexcursionistalcoi.app.database.utils

import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.User

suspend fun findUserById(userId: String): User? = ServerDatabase(
    scope = Throwable().stackTrace.firstOrNull()?.className,
    operation = "findUserById"
) { User.findById(userId) }

suspend fun findItemTypeById(itemTypeId: Int): ItemType? = ServerDatabase(
    scope = Throwable().stackTrace.firstOrNull()?.className,
    operation = "findItemTypeById"
) { ItemType.findById(itemTypeId) }

suspend fun findSectionById(sectionId: Int): Section? = ServerDatabase(
    scope = Throwable().stackTrace.firstOrNull()?.className,
    operation = "findSectionById"
) { Section.findById(sectionId) }
