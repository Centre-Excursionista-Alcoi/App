package org.centrexcursionistalcoi.app.android

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.ktor.http.*
import org.centrexcursionistalcoi.app.MainApp
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.centrexcursionistalcoi.app.push.PushNotification
import tech.kotlinlang.permission.PermissionInitiation

class MainActivity : NfcIntentHandlerActivity() {
    private val appUpdateResultLauncher = PlatformAppUpdates.registerForActivityResult(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FileKit.init(this)

        PermissionInitiation.setActivity(this)

        PlatformAppUpdates.initialize(this, appUpdateResultLauncher)

        KMPNotifier.onCreateOrOnNewIntent(intent)

        openWebOnlyLinkIfNeeded(intent)

        val pushNotification = getPushNotificationFromIntent()
        val url = getUrlFromIntent()

        setContent {
            MainApp(url, pushNotification)
        }
    }

    override fun onResume() {
        super.onResume()

        PlatformAppUpdates.checkForUpdates(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PlatformAppUpdates.stop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        KMPNotifier.onCreateOrOnNewIntent(intent)
        openWebOnlyLinkIfNeeded(intent)
    }

    /**
     * The App Link intent-filter in the manifest claims the whole `server.centrexcursionistalcoi.app` host, but some
     * paths on it (currently just the password reset flow) are self-contained server-rendered web pages with no
     * native screen to route to -- if the App Link hijacks them into the bare app shell instead of a browser, the
     * user gets stranded with no way to finish e.g. resetting their password (see #617). Open those specific paths
     * in a Custom Tab instead.
     *
     * Explicitly targeting a browser package is required, not optional: this device/App Link combination can be in
     * a domain-verification state where a plain (package-less) `ACTION_VIEW`/`CustomTabsIntent.launchUrl()` on the
     * same URL resolves straight back to this app's own MainActivity instead of a browser, looping forever
     * (confirmed on-device -- `pm get-app-links` showed this app as the resolved handler for this host).
     *
     * [CustomTabsClient.getPackageName]'s `ignoreDefault=false` path resolves the device's default browser via a
     * bare `http://` (no host) VIEW intent, which this app's own App Link intent-filter -- scoped to a specific
     * host -- never matches, so it can't resolve back to us the way the exact reset_password URL can.
     */
    private fun openWebOnlyLinkIfNeeded(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "https" || uri.host != "server.centrexcursionistalcoi.app") return
        if (uri.path !in WEB_ONLY_PATHS) return

        val customTabsIntent = CustomTabsIntent.Builder().build()
        val browserPackage = CustomTabsClient.getPackageName(this, emptyList())
            ?: resolveNonSelfViewHandler(uri)
        if (browserPackage == null) {
            log.w { "No browser found to open $uri outside of this app; ignoring App Link." }
            return
        }
        customTabsIntent.intent.setPackage(browserPackage)
        customTabsIntent.launchUrl(this, uri)
    }

    /** Falls back to any app (other than this one) that can handle a plain `ACTION_VIEW` for [url], if any. */
    private fun resolveNonSelfViewHandler(url: Uri): String? {
        val viewIntent = Intent(Intent.ACTION_VIEW, url)
        return packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .firstOrNull { it != packageName }
    }

    private fun getPushNotificationFromIntent(): PushNotification? {
        val extras = intent.extras ?: return null
        // convert the extras to a Map<String, *>
        @Suppress("DEPRECATION") val data = extras.keySet().associateWith { extras.get(it) }
        try {
            // Get the PushNotification object
            return PushNotification.fromData(data)
        } catch (_: IllegalArgumentException) {
            // Ignore invalid push notification data
            log.d { "Got intent with extras, but no valid push notification could be inferred." }
            return null
        }
    }

    private fun getUrlFromIntent(): Url? {
        val uri = intent.data ?: return null
        if (uri.scheme != "cea" || uri.host == "server.centrexcursionistalcoi.app") return null
        return Url(uri.toString())
    }

    companion object {
        private val log = logging()

        /** Paths on `server.centrexcursionistalcoi.app` that must always open as a web page. See [openWebOnlyLinkIfNeeded]. */
        private val WEB_ONLY_PATHS = setOf("/reset_password")
    }
}