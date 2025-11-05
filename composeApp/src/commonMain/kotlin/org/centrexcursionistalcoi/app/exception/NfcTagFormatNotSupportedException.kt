package org.centrexcursionistalcoi.app.exception

class NfcTagFormatNotSupportedException(
    val format: String,
) : NfcException("NFC tag cannot be formatted as $format.")
