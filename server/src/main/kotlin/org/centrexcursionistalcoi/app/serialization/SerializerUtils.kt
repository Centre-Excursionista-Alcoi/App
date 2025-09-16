package org.centrexcursionistalcoi.app.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

fun <T> KSerializer<T>.list(): KSerializer<List<T>> = ListSerializer(this)
