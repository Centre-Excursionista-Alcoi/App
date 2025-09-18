package org.centrexcursionistalcoi.app.nav

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.auth.UserData

sealed interface Destination {
    @Serializable @SerialName("loading") data object Loading : Destination
    @Serializable @SerialName("login") data object Login : Destination
    @Serializable @SerialName("home") data object Home : Destination
}
