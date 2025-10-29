package org.centrexcursionistalcoi.app.platform

import android.nfc.Tag
import kotlin.coroutines.Continuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.android.nfc.NfcUtils

actual object PlatformNFC : PlatformProvider {
    actual override val isSupported: Boolean = true // TODO: Check actual compatibility

    private val readMutex = Mutex()
    var readContinuation: Continuation<String?>? = null
        private set

    private val writeMutex = Mutex()
    var writeContinuation: Continuation<Tag>? = null
        private set

    actual suspend fun readNFC(): String? = readMutex.withLock {
        suspendCancellableCoroutine {
            readContinuation = it
            it.invokeOnCancellation {
                readContinuation = null
            }
        }.also { readContinuation = null }
    }

    actual suspend fun writeNFC(message: String) {
        writeMutex.withLock {
            val tag = suspendCancellableCoroutine {
                writeContinuation = it
                it.invokeOnCancellation {
                    writeContinuation = null
                }
            }.also { writeContinuation = null }
            NfcUtils.writeTag(message, tag)
        }
    }
}
