package org.centrexcursionistalcoi.app.nav

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Links (like `https://centrexcursionistalcoi.app/admin/lendings/<id>`, from the emails the server sends) that
 * reach the app by a callback rather than by starting it with the link, which is how iOS delivers them, whether
 * the app is being launched or is already running.
 *
 * The platform hands a link to [receive]; the app opens it once it can (see [canOpenLinks]) and then [consume]s
 * it. A link that arrives before the app is ready stays in [pending], so launching the app from a link works.
 */
object DeepLinks {
    /** The link waiting to be opened, if any. */
    val pending: StateFlow<Url?>
        field = MutableStateFlow<Url?>(null)

    /**
     * Called by the platform with a link it was asked to open. Anything that isn't a valid URL is ignored.
     */
    fun receive(url: String) {
        val parsed = try {
            Url(url)
        } catch (_: Exception) {
            return
        }
        pending.value = parsed
    }

    /** Marks [url] as handled. Does nothing if a newer link has arrived meanwhile, which stays pending. */
    fun consume(url: Url) {
        pending.compareAndSet(url, null)
    }
}

/**
 * Whether a link can be opened from here right away: not while the app is still loading or the user is logging
 * in or out. Then it has to wait until the user is in, see [org.centrexcursionistalcoi.app.nav.Destination.Main].
 */
fun Destination.canOpenLinks(): Boolean = this !is Destination.Loading && this !is Destination.Login && this !is Destination.Logout
