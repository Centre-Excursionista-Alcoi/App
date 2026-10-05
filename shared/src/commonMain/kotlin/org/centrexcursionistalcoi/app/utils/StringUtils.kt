package org.centrexcursionistalcoi.app.utils

fun String.takeUnlessEmpty(): String? = takeUnless { it.isEmpty() }
