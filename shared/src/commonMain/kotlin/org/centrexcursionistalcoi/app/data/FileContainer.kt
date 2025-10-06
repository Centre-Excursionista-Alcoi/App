package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface FileContainer {
    val files: Map<String, Uuid?>
}
