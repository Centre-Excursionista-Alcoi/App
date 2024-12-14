package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class UserD(
    val email: String,
    val isAdmin: Boolean,
    val isConfirmed: Boolean,
    val name: String,
    val familyName: String,
    val nif: String,
    val phone: String
) {
    fun vCard(): String = StringBuilder().apply {
        append("BEGIN:VCARD\n")
        append("VERSION:3.0\n")
        append("N:$familyName;$name;;;\n")
        append("FN:$name $familyName\n")
        append("TEL;TYPE=CELL,VOICE:$phone\n")
        append("EMAIL;TYPE=WORK:$email\n")
        append("X-CEA-NIF:$nif\n")
        append("END:VCARD")
    }.toString()
}
