package org.centrexcursionistalcoi.app.platform

import android.content.pm.PackageManager
import android.nfc.Tag
import android.widget.Toast
import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.android.MainActivity
import org.centrexcursionistalcoi.app.android.nfc.NfcUtils
import org.centrexcursionistalcoi.app.data.NfcPayload
import org.centrexcursionistalcoi.app.doMain
import org.centrexcursionistalcoi.app.exception.NfcException
import org.centrexcursionistalcoi.app.exception.NfcTagFormatNotSupportedException
import org.centrexcursionistalcoi.app.exception.NfcTagIsReadOnlyException
import org.centrexcursionistalcoi.app.exception.NfcTagMemorySmallException
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.Continuation

actual object PlatformNFC : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean get() {
        val context = MainActivity.instance ?: return false
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    private val readMutex = Mutex()
    var readContinuation: Continuation<NfcPayload?>? = null
        private set

    private val writeMutex = Mutex()
    var writeContinuation: Continuation<Tag>? = null
        private set

    actual suspend fun readNFC(): NfcPayload? = readMutex.withLock {
        suspendCancellableCoroutine {
            readContinuation = it
            it.invokeOnCancellation {
                readContinuation = null
            }
        }.also { readContinuation = null }
    }

    actual suspend fun writeNFC(message: String) {
        val previousReadContinuation = readContinuation
        readContinuation = null

        writeMutex.withLock {
            val tag = suspendCancellableCoroutine {
                writeContinuation = it
                it.invokeOnCancellation {
                    writeContinuation = null
                }
            }.also { writeContinuation = null }
            val responseMessage: String = try {
                val uuid = message.toUuidOrNull()
                if (uuid != null) {
                    log.d { "Writing tag as UUID..." }

                    // Get the byte array representation of the UUID
                    val data = uuid.toByteArray()

                    NfcUtils.writeNdefTag(data, tag, NfcPayload.MIME_TYPE_UUID)
                } else {
                    log.d { "Writing tag as plain text..." }
                    NfcUtils.writeNdefTag(message, tag)
                }
                getString(Res.string.nfc_write_success)
            } catch (e: NfcTagIsReadOnlyException) {
                log.e(e) { "NFC tag is read-only." }
                getString(Res.string.nfc_error_read_only)
            } catch (e: NfcTagMemorySmallException) {
                log.e(e) { "NFC tag doesn't have enough memory." }
                getString(Res.string.nfc_error_too_small)
            } catch (e: NfcTagFormatNotSupportedException) {
                log.e(e) { "NFC tag doesn't support the ${e.format} format." }
                getString(Res.string.nfc_error_format_unsupported, e.format)
            } catch (e: NfcException) {
                log.e(e) { "An unknown error occurred while writing the NFC tag." }
                getString(Res.string.nfc_error_unknown, e.message ?: e::class.simpleName ?: "NfcException")
            }
            val context = MainActivity.instance ?: return
            doMain {
                Toast.makeText(context, responseMessage, Toast.LENGTH_LONG).show()
            }
        }

        readContinuation = previousReadContinuation
    }
}
