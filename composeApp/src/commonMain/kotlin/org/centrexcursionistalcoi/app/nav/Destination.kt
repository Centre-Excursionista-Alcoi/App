package org.centrexcursionistalcoi.app.nav

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable @SerialName("loading") data class Loading(
        private val _redirectTo: String? = null
    ) : Destination {
        constructor(redirectTo: Destination) : this(redirectTo.name)

        val redirectTo : Destination? get() = _redirectTo?.let { fromName(it) }
    }
    @Serializable @SerialName("login") data object Login : Destination
    @Serializable @SerialName("home") data object Home : Destination

    val name: String get() = this::class.simpleName ?: error("Unnamed class")

    companion object {
        fun fromName(name: String): Destination = when (name) {
            Loading().name -> Loading()
            Login.name -> Login
            Home.name -> Home
            else -> error("Unknown destination name: $name")
        }
    }
}
