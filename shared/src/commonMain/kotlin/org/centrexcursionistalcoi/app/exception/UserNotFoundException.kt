package org.centrexcursionistalcoi.app.exception

class UserNotFoundException(sub: String): NoSuchElementException("User with sub '$sub' not found")
