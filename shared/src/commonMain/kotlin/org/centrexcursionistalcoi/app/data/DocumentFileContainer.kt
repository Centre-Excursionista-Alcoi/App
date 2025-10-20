package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface DocumentFileContainer : FileContainer {
    val documentFile: Uuid?
}
