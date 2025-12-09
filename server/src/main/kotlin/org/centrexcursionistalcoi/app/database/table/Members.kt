package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object Members : UIntIdTable("members") {
    val status = enumeration("status", Status::class).nullable()

    val fullName = text("full_name")
    val nif = text("nif").nullable().index("idx_members_nif", isUnique = true)
    val email = text("email").nullable().index("idx_members_email", isUnique = true)

    enum class Status(val stringDef: String) {
        ACTIVE("Alta"),
        INACTIVE("Baixa"),
        PENDING("Pendent");

        companion object {
            fun parse(value: String?): Status? = value?.let {
                entries.firstOrNull { it.stringDef.equals(value, ignoreCase = true) }
            }
        }
    }
}
