package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface DocumentFileContainer : FileContainer {
    val documentFile: Uuid?

    override val files: Map<String, Uuid?> get() = mapOf("documentFile" to documentFile)
}
