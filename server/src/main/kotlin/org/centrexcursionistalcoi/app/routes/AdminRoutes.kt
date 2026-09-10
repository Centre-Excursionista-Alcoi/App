package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.AdminFileSummary
import org.centrexcursionistalcoi.app.data.AdminUserSummary
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.sendPasswordRecoveryEmail
import org.centrexcursionistalcoi.app.request.ForcePasswordChangeRequest
import org.centrexcursionistalcoi.app.response.PagedResponse
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.translation.locale
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

private fun parseLimit(raw: String?): Int = (raw?.toIntOrNull() ?: 50).coerceIn(1, 200)
private fun parseOffset(raw: String?): Int = (raw?.toIntOrNull() ?: 0).coerceAtLeast(0)

/**
 * JSON REST API backing the admin panel's Kilua frontend, mounted under `/admin/api`. Every handler
 * independently calls [assertAdmin], on top of `/admin`'s own server-side gate in [adminUiRoutes] -- so the
 * API is safe to call even if it were ever reachable by some other path.
 */
fun Route.adminApiRoutes() {
    // Lists rows of the `files` table (name/type/size/lastModified only -- never the file bytes themselves,
    // which would make listing every file in the database prohibitively expensive), searchable by name.
    get("/files") {
        assertAdmin() ?: return@get

        val query = call.request.queryParameters["q"]?.trim()?.takeIf { it.isNotEmpty() }
        val limit = parseLimit(call.request.queryParameters["limit"])
        val offset = parseOffset(call.request.queryParameters["offset"])

        // SQL-side length(bytes) instead of FileEntity.all()/toData(), which would deserialize every file's
        // full content into memory just to list it.
        val sizeExpression = CustomFunction("length", LongColumnType(), Files.bytes)
        val filter: Op<Boolean> = query?.let { Files.name.lowerCase() like "%${it.lowercase()}%" } ?: Op.TRUE

        val (items, total) = Database {
            val rows = Files
                .select(Files.id, Files.name, Files.type, Files.lastModified, sizeExpression)
                .where { filter }
                .orderBy(Files.lastModified)
                .limit(limit)
                .offset(offset.toLong())
                .map { row ->
                    AdminFileSummary(
                        id = row[Files.id].value.toKotlinUuid(),
                        name = row[Files.name],
                        type = row[Files.type],
                        sizeBytes = row[sizeExpression],
                        lastModified = row[Files.lastModified].toKotlinInstant(),
                    )
                }
            val count = Files.selectAll().where { filter }.count()
            rows to count
        }

        call.respond(PagedResponse(items, total, limit, offset))
    }

    // Lists users (lightweight summary, not the fully-hydrated UserData used by /users), searchable by
    // name/email/NIF.
    get("/users") {
        assertAdmin() ?: return@get

        val query = call.request.queryParameters["q"]?.trim()?.takeIf { it.isNotEmpty() }
        val limit = parseLimit(call.request.queryParameters["limit"])
        val offset = parseOffset(call.request.queryParameters["offset"])

        val filter: Op<Boolean> = query?.let {
            val pattern = "%${it.lowercase()}%"
            (UserReferences.fullName.lowerCase() like pattern) or
                (UserReferences.email.lowerCase() like pattern) or
                (UserReferences.nif.lowerCase() like pattern)
        } ?: Op.TRUE

        val (items, total) = Database {
            val rows = UserReferences
                .select(
                    UserReferences.sub,
                    UserReferences.memberNumber,
                    UserReferences.fullName,
                    UserReferences.email,
                    UserReferences.groups,
                    UserReferences.isDisabled,
                )
                .where { filter }
                .orderBy(UserReferences.fullName)
                .limit(limit)
                .offset(offset.toLong())
                .map { row ->
                    AdminUserSummary(
                        sub = row[UserReferences.sub].value,
                        memberNumber = row[UserReferences.memberNumber],
                        fullName = row[UserReferences.fullName],
                        email = row[UserReferences.email],
                        groups = row[UserReferences.groups],
                        isDisabled = row[UserReferences.isDisabled],
                    )
                }
            val count = UserReferences.selectAll().where { filter }.count()
            rows to count
        }

        call.respond(PagedResponse(items, total, limit, offset))
    }

    // Admin-forced password change: sets the user's password directly (mirroring /reset_password's logic)
    // and notifies them by email, without requiring a recovery-request round trip.
    post("/users/{sub}/force-password") {
        assertAdmin() ?: return@post

        val sub = call.parameters["sub"]!!
        val reference = Database { UserReferenceEntity.find { UserReferences.sub eq sub }.firstOrNull() }
        if (reference == null) {
            respondError(Error.UserNotFound())
            return@post
        }

        val request = try {
            call.receive<ForcePasswordChangeRequest>()
        } catch (_: Exception) {
            respondError(Error.MalformedRequest())
            return@post
        }

        val newPassword = request.newPassword.trim().toCharArray()
        if (!Passwords.isSafe(newPassword)) {
            respondError(Error.PasswordNotSafeEnough())
            return@post
        }

        val hashedPassword = Passwords.hash(newPassword)
        Database { reference.password = hashedPassword }

        val locale = call.request.locale()
        Email.sendTemplate(
            to = listOf(MailerSendEmail(reference.email, reference.fullName)),
            template = EmailTemplate.PasswordChangedNotification,
            locale = locale,
            args = mapOf("userName" to reference.fullName),
        )

        call.respond(HttpStatusCode.NoContent)
    }

    // Admin-triggered version of /lost_password: sends the same recovery email, without the user having to
    // request it themselves.
    post("/users/{sub}/send-recovery-email") {
        assertAdmin() ?: return@post

        val sub = call.parameters["sub"]!!
        val reference = Database { UserReferenceEntity.find { UserReferences.sub eq sub }.firstOrNull() }
        if (reference == null) {
            respondError(Error.UserNotFound())
            return@post
        }

        sendPasswordRecoveryEmail(call, reference)

        // NoContent (not Accepted/OK), matching Kilua's RestClient, which treats exactly this status as the
        // "empty body" case and skips trying to JSON-decode a response that never has one.
        call.respond(HttpStatusCode.NoContent)
    }
}

/**
 * A minimal, hand-written login page -- deliberately NOT part of the Kilua bundle -- served to any
 * unauthenticated (or non-admin) request under `/admin`. Posts credentials to the existing `/login` route
 * (form-urlencoded, same as the mobile/desktop clients) and reloads on success.
 */
private const val ADMIN_LOGIN_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>CEA Admin - Login</title>
    <style>
        body { font-family: system-ui, sans-serif; background: #f4f5f7; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
        form { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 320px; }
        h1 { font-size: 1.25rem; margin-top: 0; }
        label { display: block; margin-top: 1rem; font-size: 0.875rem; color: #333; }
        input { width: 100%; padding: 0.5rem; margin-top: 0.25rem; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
        button { margin-top: 1.5rem; width: 100%; padding: 0.6rem; background: #2b6cb0; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button:disabled { opacity: 0.6; cursor: default; }
        #error { color: #c53030; font-size: 0.875rem; margin-top: 1rem; display: none; }
    </style>
</head>
<body>
    <form id="login-form">
        <h1>CEA Admin</h1>
        <label>Email<input type="email" id="email" required autocomplete="username"></label>
        <label>Password<input type="password" id="password" required autocomplete="current-password"></label>
        <button type="submit">Log in</button>
        <p id="error"></p>
    </form>
    <script>
        document.getElementById('login-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = event.target.querySelector('button');
            const errorEl = document.getElementById('error');
            errorEl.style.display = 'none';
            button.disabled = true;
            try {
                const body = new URLSearchParams();
                body.set('email', document.getElementById('email').value);
                body.set('password', document.getElementById('password').value);
                const response = await fetch('/login', { method: 'POST', body, credentials: 'same-origin' });
                if (response.ok) {
                    window.location.reload();
                } else {
                    errorEl.textContent = 'Login failed. Check your credentials, or make sure this account is an admin.';
                    errorEl.style.display = 'block';
                }
            } finally {
                button.disabled = false;
            }
        });
    </script>
</body>
</html>"""

/**
 * Serves the admin panel's UI under `/admin`: the built Kilua bundle (and any of its static assets) for a
 * logged-in admin, or [ADMIN_LOGIN_HTML] for anyone else -- so an unauthenticated request never receives the
 * app bundle at all, only ever this login page.
 */
fun Route.adminUiRoutes() {
    get("{path...}") {
        val session = getUserSession()
        if (session?.isAdmin() != true) {
            call.respondText(ADMIN_LOGIN_HTML, ContentType.Text.Html)
            return@get
        }

        val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()

        // The Kilua browserRouter's own "unmatched route" fallback (`view`/`defaultContent`) renders
        // unconditionally alongside whatever named route also matches, rather than exclusively as a fallback
        // -- so the bare /admin root is redirected here, server-side, to a real named client route instead of
        // relying on that.
        if (path.isEmpty()) {
            call.respondRedirect("/admin/files")
            return@get
        }

        val classLoader = call.application.environment.classLoader
        val resourcePath = "admin-static/$path"
        val resourceBytes = classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes() }

        if (resourceBytes != null) {
            call.respondBytes(resourceBytes, ContentType.defaultForFilePath(resourcePath))
            return@get
        }

        // No static asset at this path -- most likely a Kilua browserRouter client-side route (e.g.
        // /admin/files) being loaded/refreshed directly, so fall back to the SPA shell.
        val indexBytes = classLoader.getResourceAsStream("admin-static/index.html")?.use { it.readBytes() }
        if (indexBytes == null) {
            call.respondText(
                "Admin panel bundle is not built. Run :admin:composeCompatibilityBrowserDistribution and rebuild the server.",
                status = HttpStatusCode.ServiceUnavailable,
            )
        } else {
            call.respondBytes(indexBytes, ContentType.Text.Html)
        }
    }
}
