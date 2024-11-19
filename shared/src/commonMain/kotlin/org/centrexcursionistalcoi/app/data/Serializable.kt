package org.centrexcursionistalcoi.app.data

/**
 * Interface that marks a class as serializable.
 */
interface Serializable<SerializableType> {
    /**
     * Convert the object to a serializable type.
     */
    fun serializable(): SerializableType
}
