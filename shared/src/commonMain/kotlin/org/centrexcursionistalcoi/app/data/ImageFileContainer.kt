package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface ImageFileContainer : FileContainer {
    val image: Uuid?
}
