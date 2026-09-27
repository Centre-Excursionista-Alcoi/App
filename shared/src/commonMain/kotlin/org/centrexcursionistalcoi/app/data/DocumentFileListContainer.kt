package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface DocumentFileListContainer {
    val documentFiles: List<Uuid>
}
