package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface ImageFileListContainer {
    val images: List<Uuid>
}
