package org.centrexcursionistalcoi.app.exception

@Suppress("CanBeParameter")
class NfcTagMemorySmallException(
    val messageSize: Int? = null,
    val tagCapacity: Int? = null,
) : NfcException("Message is too large for this tag. Required size: $messageSize bytes, Tag capacity: $tagCapacity bytes.")
