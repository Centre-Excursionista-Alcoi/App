package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Member
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object Members : UIntIdTable("members") {
    val status = enumeration("status", Member.Status::class).nullable()

    val fullName = text("full_name")
    val nif = text("nif").nullable().index("idx_members_nif", isUnique = true)
    val email = text("email").nullable().index("idx_members_email", isUnique = true)
}
