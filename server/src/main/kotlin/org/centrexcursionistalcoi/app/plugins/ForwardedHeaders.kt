package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

/**
 * The app is only ever reachable through the nginx reverse proxy (see the configs under `reverse-proxy/nginx`),
 * which sets
 * `X-Real-IP`/`X-Forwarded-For` via `$proxy_add_x_forwarded_for` -- appending the real client address to whatever
 * `X-Forwarded-For` the client itself sent, rather than replacing it. Without this plugin, `ApplicationRequest.origin`
 * (e.g. `call.request.origin.remoteHost`, used when recording auth events -- see `Auth.kt`) reads the raw socket
 * peer instead, which is always nginx's own address.
 *
 * [useLastProxy] trusts only the *last* entry in `X-Forwarded-For` -- the one nginx itself appended -- so a client
 * can't spoof its recorded IP by sending its own `X-Forwarded-For` header (the default [XForwardedHeaders] config,
 * [io.ktor.server.plugins.forwardedheaders.XForwardedHeadersConfig.useFirstProxy], trusts the *first* entry instead,
 * which here would be exactly that spoofed value).
 */
fun Application.configureForwardedHeaders() {
    install(XForwardedHeaders) {
        useLastProxy()
    }
}
