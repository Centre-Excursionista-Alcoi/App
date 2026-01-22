package org.centrexcursionistalcoi.app.exception

import kotlin.uuid.Uuid

class MissingCrossReferenceException(val type: String, val id: String) : NoSuchElementException("Could not find cross reference: $type#$id") {
    constructor(type: String, id: Uuid) : this(type, id.toString())
}
