package org.centrexcursionistalcoi.app.android.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import androidx.core.content.IntentCompat
import java.io.IOException

/**
 * A singleton utility object for handling NFC read and write operations.
 */
object NfcUtils {

    private const val MIME_TYPE = "application/vnd.org.centrexcursionistalcoi.nfc"

    /**
     * Reads NDEF messages from a standard NFC tag intent.
     * Renamed from readTag for clarity.
     */
    fun readNdefTag(intent: Intent): List<String> {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action) {

            val messages = IntentCompat.getParcelableArrayExtra(intent, NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            if (messages != null) {
                return messages.flatMap { parcelable ->
                    val ndefMessage = parcelable as NdefMessage
                    ndefMessage.records.map { record ->
                        String(record.payload)
                    }
                }
            }
        }
        return emptyList()
    }

    /**
     * NEW: Reads data from a specific block of a Mifare Classic tag.
     *
     * @param tag The NFC tag object obtained from the intent.
     * @return A string with the result of the read operation.
     */
    fun readMifareClassicTag(tag: Tag?): String {
        val mifare = MifareClassic.get(tag) ?: return "Error: Not a Mifare Classic tag."

        try {
            mifare.connect()
            val sectorToRead = 1 // Example: reading from sector 1

            // Authenticate the sector with a known key.
            // MifareClassic.KEY_DEFAULT is a common key for new/unformatted tags.
            val isAuthenticated = mifare.authenticateSectorWithKeyA(sectorToRead, MifareClassic.KEY_DEFAULT)

            if (isAuthenticated) {
                // Each sector has 4 blocks. Calculate the index of the first block in the sector.
                val blockIndex = mifare.sectorToBlock(sectorToRead)
                // Read a specific block (e.g., the first block in the sector)
                val blockData: ByteArray = mifare.readBlock(blockIndex)

                // Convert byte array to a hexadecimal string for display
                val hexString = blockData.joinToString("") { "%02x".format(it) }.uppercase()
                return "Mifare Classic Read Success:\nSector $sectorToRead, Block $blockIndex\nData: $hexString"
            } else {
                return "Error: Sector authentication failed."
            }
        } catch (e: IOException) {
            return "Error reading tag: ${e.message}"
        } finally {
            try {
                mifare.close()
            } catch (e: IOException) {
                // Ignore close exception
            }
        }
    }

    /**
     * Writes data to an NFC tag.
     *
     * @param message The string message to write to the tag.
     * @param tag The NFC tag object obtained from the intent.
     * @return A string indicating the result: "Success", "Error: Tag is not NDEF formatable", etc.
     */
    fun writeNdefTag(message: String, tag: Tag?): String {
        val ndefMessage = NdefMessage(
            arrayOf(NdefRecord.createMime(MIME_TYPE, message.toByteArray(Charsets.US_ASCII)))
        )
        return try {
            val ndef = Ndef.get(tag)
            ndef?.let {
                it.connect()
                if (!it.isWritable) {
                    return "Error: Tag is read-only."
                }
                if (it.maxSize < ndefMessage.toByteArray().size) {
                    return "Error: Message is too large for this tag."
                }
                it.writeNdefMessage(ndefMessage)
                "Success: Wrote message to tag."
            } ?: "Error: Tag is not NDEF formatable."
        } catch (e: Exception) {
            "Error: Failed to write to tag. ${e.message}"
        }
    }
}
