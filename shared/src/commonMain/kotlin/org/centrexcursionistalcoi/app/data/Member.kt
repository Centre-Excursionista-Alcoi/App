package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val memberNumber: UInt,
    val status: Status? = null,
    val fullName: String,
    val nif: String? = null,
    val email: String? = null,
): Entity<UInt> {
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

    override val id: UInt get() = memberNumber

    override fun toMap(): Map<String, Any?> = mapOf(
        "memberNumber" to memberNumber,
        "status" to status,
        "fullName" to fullName,
        "nif" to nif,
        "email" to email,
    )

    /**
     * Returns a copy of this member with sensitive fields removed.
     */
    fun strip() = copy(nif = null, email = null, status = null)
}
