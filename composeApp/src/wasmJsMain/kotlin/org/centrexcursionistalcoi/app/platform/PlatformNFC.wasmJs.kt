@file:OptIn(ExperimentalWasmJsInterop::class)

package org.centrexcursionistalcoi.app.platform

import io.github.aakira.napier.Napier
import io.ktor.client.fetch.AbortSignal
import io.ktor.client.fetch.ArrayBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.DataView
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

external interface NDEFOptions : JsAny {
    /**
     * An [AbortSignal] that allows the current write operation to be canceled.
     */
    var signal: AbortSignal?
}

external interface NDEFWriteOptions : NDEFOptions {
    /**
     * A boolean value specifying whether or not existing records should be overwritten, if such exists.
     */
    var overwrite: Boolean?
}

external interface NDEFMessage : JsAny {
    /**
     * An array of [NDEFRecord]s contained in the NDEF message.
     */
    val records: JsArray<NDEFRecord>
}

external interface NDEFEvent : JsAny {
    /**
     * The serial number of the NFC tag being read.
     */
    val serialNumber: String

    /**
     * The NDEF message read from the NFC tag.
     */
    val message: NDEFMessage
}

external interface NDEFRecord : JsAny {
    /**
     * Contains the data to be transmitted, a [String], an [ArrayBuffer], a [JsArray], a [DataView], or an array of nested [NDEFRecord]s.
     */
    val data: JsAny?

    /**
     * A string specifying the record's encoding.
     */
    val encoding: String?

    /**
     * A developer-defined identifier for the record.
     */
    val id: String?

    /**
     * A valid BCP 47 language tag.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Glossary/BCP_47_language_tag">BCP 47 language tags</a>
     */
    val lang: String?

    /**
     * A valid MIME type.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/MIME_types">MIME types</a>
     */
    val mediaType: String?
}

external class NDEFReader() : EventTarget {
    /**
     * Activates a reading device and returns a [Promise] that either resolves when an NFC tag read operation is scheduled or rejects if a hardware or permission error is
     * encountered.
     *
     * This method triggers a permission prompt if the "nfc" permission has not been previously granted.
     */
    fun scan(options: NDEFOptions = definedExternally): Promise<JsAny>

    /**
     * Attempts to write an NDEF message to a tag and returns a [Promise] that either resolves when a message has been written to the tag or rejects if a hardware or permission
     * error is encountered.
     *
     * This method triggers a permission prompt if the "nfc" permission has not been previously granted.
     * @param message The NDEF message to write to the tag. Can be a [String], [ArrayBuffer], a [JsArray], [DataView] or a [JsArray] of [NDEFRecord]s.
     * @param options Additional options for the write operation.
     *
     * @return A [Promise] that either resolves when a message has been written to the tag or rejects if a hardware or permission error is encountered.
     */
    fun write(message: JsAny, options: NDEFWriteOptions = definedExternally): Promise<JsAny>

    var onreadingerror: ((event: Event) -> Unit)?
    var onreading: ((event: Event) -> Unit)?
}

val ndefReaderAvailable: Boolean = js("(\"NDEFReader\" in window)")

actual object PlatformNFC {
    actual val supportsNFC: Boolean
        get() = ndefReaderAvailable

    actual suspend fun readNFC(): String? = suspendCoroutine { cont ->
        val ndef = NDEFReader()
        ndef.scan()
            .then {
                ndef.addEventListener("readingerror") {
                    Napier.e { "Cannot read data from NFC tag." }
                }
                ndef.addEventListener("reading") { ev ->
                    val event = ev as NDEFEvent
                    val records = event.message.records
                    Napier.d { "Read NFC tag. Serial number: ${event.serialNumber}. Record count: ${records.length}" }
                    cont.resume(records.toList().mapNotNull { it.data?.toString() }.joinToString("\n"))
                }
                null
            }
            .catch { error ->
                val throwable = error.toThrowableOrNull()
                Napier.e(throwable) { "Could not read NFC: $error" }
                cont.resumeWithException(throwable ?: RuntimeException("Could not read NFC: $error"))
                null
            }
    }

    actual suspend fun writeNFC(message: String) {
        val ndef = NDEFReader()
        ndef.write(message.toJsString()).await<JsAny>()
    }
}
