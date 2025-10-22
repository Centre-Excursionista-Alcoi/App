package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

interface SubReferencedFileContainer {
    /**
     * Holds a list of triples where each triple consists of:
     * - The file name property.
     * - The file ID (of type Uuid?) which may be null if the file doesn't exist.
     * - The namespace to use for the file.
     */
    val referencedFiles: List<Triple<String, Uuid?, String>>
}
