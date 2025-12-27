package org.centrexcursionistalcoi.app.android

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.android.nfc.NfcUtils
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class NfcIntentHandlerActivity : AppCompatActivity() {
    companion object {
        private val log = logging()
    }

    protected var nfcAdapter: NfcAdapter? = null
    protected var pendingIntent: PendingIntent? = null
    protected var intentFiltersArray: Array<IntentFilter>? = null

    private val techList = arrayOf(
        arrayOf(Ndef::class.java.name),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.d { "Getting NFC adapter..." }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            log.e { "NFC not available" }
        } else {
            log.d { "Preparing Activity for NFC support..." }

            // Create a PendingIntent to handle NFC intents
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            // Set up an intent filter for NDEF discovered
            val ndefIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            intentFiltersArray = arrayOf(ndefIntentFilter)

            // Handle the intent that started the activity
            handleIntent(getIntent())
        }
    }

    override fun onResume() {
        super.onResume()

        pendingIntent ?: return
        intentFiltersArray ?: return

        // Enable foreground dispatch to intercept NFC tags
        log.d { "Enabling NFC foreground dispatch..." }
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techList)
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch when the activity is not in the foreground
        nfcAdapter?.disableForegroundDispatch(this)?.also {
            log.d { "Disabled NFC foreground dispatch" }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // This is called when an NFC tag is discovered while the app is running
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {

            log.d { "Handling NFC intent..." }

            // Store the tag for writing
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                // Handle write
                PlatformNFC.writeContinuation?.resume(tag) ?: log.v { "There's no pending write continuation." }

                // Handle read
                val techList = tag.techList.map { it.substringAfterLast('.') }

                val payload = if ("Ndef" in techList) {
                    // Priority given to NDEF-formatted tags
                    val tagPayload = NfcUtils.readNdefTag(intent)
                    if (tagPayload == null) {
                        PlatformNFC.readContinuation?.resumeWithException(
                            NoSuchElementException("NFC tag has no NDEF records.")
                        )
                        return
                    }
                    tagPayload
                } else {
                    PlatformNFC.readContinuation?.resumeWithException(
                        UnsupportedOperationException("Unsupported NFC tag type: $techList")
                    )
                    return
                }

                log.d { "Read NFC tag: $payload" }
                PlatformNFC.readContinuation?.resume(payload) ?: log.v { "There's no pending read continuation." }
            }
        }
    }
}