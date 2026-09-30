package org.centrexcursionistalcoi.app.storage.fs

import kotlinx.io.files.Path

fun Path.toFile(): java.io.File = java.io.File(toString())
