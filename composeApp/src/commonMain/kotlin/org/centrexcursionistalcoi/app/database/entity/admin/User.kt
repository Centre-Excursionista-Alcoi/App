package org.centrexcursionistalcoi.app.database.entity.admin

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.centrexcursionistalcoi.app.data.UserD

@Entity
data class User(
    @PrimaryKey val email: String,
    val isAdmin: Boolean,
    val isConfirmed: Boolean,
    val name: String,
    val familyName: String,
    val nif: String,
    val phone: String
) {
    companion object {
        fun deserialize(userD: UserD) = User(
            email = userD.email,
            isAdmin = userD.isAdmin,
            isConfirmed = userD.isConfirmed,
            name = userD.name,
            familyName = userD.familyName,
            nif = userD.nif,
            phone = userD.phone
        )
    }
}
