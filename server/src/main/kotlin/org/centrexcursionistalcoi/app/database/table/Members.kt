package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Member
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object Members : UIntIdTable("members") {
    val status = enumeration("status", Member.Status::class).nullable()

    val fullName = text("full_name")
    val nif = text("nif").nullable().index("idx_members_nif", isUnique = true)
    val email = text("email").nullable().index("idx_members_email", isUnique = true)

    @Deprecated("Use shared member status.", ReplaceWith("Member.Status", "org.centrexcursionistalcoi.app.data.Member"))
    enum class Status(val stringDef: String) {
        ACTIVE("Alta"),
        INACTIVE("Baixa"),
        PENDING("Pendent");

        companion object {
            @Deprecated("Use shared member status.", ReplaceWith("Member.Status.parse(value)", "org.centrexcursionistalcoi.app.data.Member"))
            fun parse(value: String?): Status? = value?.let {
                entries.firstOrNull { it.stringDef.equals(value, ignoreCase = true) }
            }
        }
    }
}
