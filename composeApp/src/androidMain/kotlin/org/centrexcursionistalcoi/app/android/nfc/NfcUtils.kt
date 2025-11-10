package org.centrexcursionistalcoi.app.android.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.core.content.IntentCompat
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.data.NfcPayload
import org.centrexcursionistalcoi.app.exception.NfcException
import org.centrexcursionistalcoi.app.exception.NfcTagFormatNotSupportedException
import org.centrexcursionistalcoi.app.exception.NfcTagIsReadOnlyException
import org.centrexcursionistalcoi.app.exception.NfcTagMemorySmallException

/**
 * A singleton utility object for handling NFC read and write operations.
 */
object NfcUtils {

    /**
     * Reads NDEF messages from a standard NFC tag intent.
     * @return The [NfcPayload] containing the tag ID and a list of MIME type and payload pairs, or `null` if no NDEF messages are found.
     */
    fun readNdefTag(intent: Intent): NfcPayload? {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {
            val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
            val pairs = IntentCompat.getParcelableArrayExtra(intent, NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
                ?.flatMap { parcelable ->
                    val ndefMessage = parcelable as NdefMessage
                    ndefMessage.records.map { it.toMimeType() to it.payload }
                }
            return NfcPayload(tag?.id, pairs.orEmpty())
        }
        return null
    }

    /**
     * Writes data to an NFC tag.
     *
     * @param message The string message to write to the tag.
     * @param tag The NFC tag object obtained from the intent.
     * @param mimeType The MIME type of the data being written.
     *
     * @throws NfcTagIsReadOnlyException if the tag is read-only.
     * @throws NfcTagMemorySmallException if the tag does not have enough memory.
     * @throws NfcTagFormatNotSupportedException if the tag does not support NDEF
     * @throws NfcException if writing fails for various reasons.
     */
    fun writeNdefTag(message: String, tag: Tag, mimeType: String = NfcPayload.MIME_TYPE_GENERIC) {
        val data = message.toByteArray(Charsets.US_ASCII)
        writeNdefTag(data, tag, mimeType)
    }

    /**
     * Writes data to an NFC tag.
     *
     * @param data The bytes to write to the tag.
     * @param tag The NFC tag object obtained from the intent.
     * @param mimeType The MIME type of the data being written.
     *
     * @throws NfcTagIsReadOnlyException if the tag is read-only.
     * @throws NfcTagMemorySmallException if the tag does not have enough memory.
     * @throws NfcTagFormatNotSupportedException if the tag does not support NDEF
     * @throws NfcException if writing fails for various reasons.
     */
    fun writeNdefTag(
        data: ByteArray,
        tag: Tag,
        mimeType: String = NfcPayload.MIME_TYPE_GENERIC,
    ) {
        val record = if (mimeType == "text/plain") {
            Napier.d { "Writing tag as plain-text." }
                NdefRecord.createTextRecord("en", String(data, Charsets.UTF_8))
        } else {
            NdefRecord.createMime(mimeType, data)
        }
        val ndefMessage = NdefMessage(
            arrayOf(record)
        )
        Napier.d { "Writing Ndef tag (${ndefMessage.byteArrayLength} bytes) with type $mimeType." }
        try {
            Ndef.get(tag)?.let { tagTech ->
                tagTech.connect()
                if (!tagTech.isWritable) {
                    throw NfcTagIsReadOnlyException()
                }
                val messageSize = ndefMessage.byteArrayLength
                if (tagTech.maxSize < messageSize) {
                    throw NfcTagMemorySmallException(messageSize, tagTech.maxSize)
                }
                tagTech.writeNdefMessage(ndefMessage)
            } ?: throw NfcTagFormatNotSupportedException("Ndef")
        } catch (e: Exception) {
            throw NfcException("Failed to write NDEF tag", e)
        }
    }
}
