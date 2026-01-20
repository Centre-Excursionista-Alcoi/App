package org.centrexcursionistalcoi.app.test

sealed class LoginType {
    data object NONE : LoginType()
    data object USER : LoginType()
    data object ADMIN : LoginType()
    data object LENDING_USER : LoginType()
}
