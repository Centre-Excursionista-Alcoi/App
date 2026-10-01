package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.readAvailable
import java.security.MessageDigest
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.toJavaInstant
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.utils.escapeHtml
import org.centrexcursionistalcoi.app.verification.DocumentType
import org.centrexcursionistalcoi.app.verification.DocumentVerification
import org.centrexcursionistalcoi.app.verification.DocumentVerification.toHex
import org.centrexcursionistalcoi.app.verification.VerifiedDocument

/** Uploads larger than this can't be a generated document: they're much smaller. */
private const val MAX_UPLOAD_SIZE = 20L * 1024 * 1024

/** Where the club is: dates are shown in its time zone. */
private val CLUB_TIME_ZONE = ZoneId.of("Europe/Madrid")

/**
 * What the verification page shows, besides its form.
 */
private enum class VerifyState {
    /** Just the form. */
    Form,
    /** Neither a code nor a file was given. */
    NoInput,
    InvalidCode,
    /** No document was generated with the code. */
    NotFound,
    /** The code is a document's, and no file was given to compare. */
    Found,
    /** The file is the document with the code (or, without a code, one generated here). */
    Match,
    /** The file isn't the document with the code: it was changed, or it's another one. */
    Mismatch,
    /** Without a code: no document generated here is that file. */
    UnknownFile,
    TooLarge,
}

/**
 * The page to verify documents the server generated (see [DocumentVerification]): with the code printed on them,
 * their file, or both. Never shows anything about the document beyond what it is and when it was generated.
 */
@OptIn(ExperimentalXmlUtilApi::class)
private object VerifyPage : WebTemplate("verify") {
    fun translation(locale: Locale, key: String): String = translationsBook[locale][key]

    override fun DocumentRedactor.render(args: Map<String, String?>): String {
        val state = VerifyState.valueOf(args.getValue("state")!!)
        val code = args["code"].orEmpty()

        val result = when (state) {
            VerifyState.Form -> null
            VerifyState.NoInput -> "error" to t("no_input")
            VerifyState.InvalidCode -> "error" to t("invalid_code")
            VerifyState.NotFound -> "error" to t("not_found")
            VerifyState.Found -> "info" to t("found")
            VerifyState.Match -> "success" to t("match")
            VerifyState.Mismatch -> "error" to t("mismatch")
            VerifyState.UnknownFile -> "error" to t("unknown_file")
            VerifyState.TooLarge -> "error" to t("too_large")
        }
        val details = args["document_code"]?.let {
            """
            <dl class="details">
                <dt>${t("detail_type")}</dt><dd>${args["document_type"].orEmpty().escapeHtml()}</dd>
                <dt>${t("detail_generated")}</dt><dd>${args["document_generated"].orEmpty().escapeHtml()}</dd>
                <dt>${t("detail_code")}</dt><dd><code>${it.escapeHtml()}</code></dd>
                <dt>${t("detail_sha256")}</dt><dd><code class="hash">${args["document_sha256"].orEmpty().escapeHtml()}</code></dd>
            </dl>
            """.trimIndent()
        }.orEmpty()

        return """
        <!doctype html>
        <html lang="${locale.language.escapeHtml()}">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="robots" content="noindex">
            <title>${t("title")}</title>
            <link rel="stylesheet" href="/static/app-links.css">
            <link rel="stylesheet" href="/static/verify.css">
        </head>
        <body>
            <div class="card">
                <img class="icon" src="/static/app-icon.png" alt="">
                <h1>${t("title")}</h1>
                <p>${t("intro")}</p>
                ${result?.let { (kind, text) -> """<p class="result $kind" role="status">$text</p>""" }.orEmpty()}
                $details
                ${if (state == VerifyState.Found) "<p>${t("upload_hint")}</p>" else ""}
                <form method="post" action="/verify" enctype="multipart/form-data">
                    <label for="code">${t("code_label")}</label>
                    <input id="code" name="code" type="text" value="${code.escapeHtml()}" placeholder="ABCD-EFGH-JKMN" autocomplete="off" autocapitalize="characters" spellcheck="false">
                    <label for="file">${t("file_label")}</label>
                    <input id="file" name="file" type="file" accept="application/pdf">
                    <button class="cta" type="submit">${t("submit")}</button>
                </form>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}

private suspend fun RoutingContext.respondVerifyPage(
    state: VerifyState,
    code: String?,
    document: VerifiedDocument? = null,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val locale = call.request.locale()
    val html = VerifyPage.render(
        locale,
        mapOf(
            "state" to state.name,
            "code" to code,
            "document_code" to document?.code,
            "document_type" to document?.let { typeName(it.type, locale) },
            "document_generated" to document?.let {
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                    .withLocale(locale)
                    .withZone(CLUB_TIME_ZONE)
                    .format(it.generatedAt.toJavaInstant())
            },
            "document_sha256" to document?.sha256,
        ),
    )
    call.respondText(html, ContentType.Text.Html, status)
}

@OptIn(ExperimentalXmlUtilApi::class)
private fun typeName(type: DocumentType, locale: Locale): String = VerifyPage.translation(
    locale,
    when (type) {
        DocumentType.MEMORY -> "type_memory"
    },
)

fun Route.verifyRoutes() {
    // Linked from every generated document, see DocumentVerification.url
    get("/verify") {
        val input = call.request.queryParameters["code"]
        if (input.isNullOrBlank()) return@get respondVerifyPage(VerifyState.Form, null)

        val code = DocumentVerification.normalize(input)
            ?: return@get respondVerifyPage(VerifyState.InvalidCode, input, status = HttpStatusCode.BadRequest)
        val document = Database { DocumentVerification.find(code) }
            ?: return@get respondVerifyPage(VerifyState.NotFound, input, status = HttpStatusCode.NotFound)
        respondVerifyPage(VerifyState.Found, document.code, document)
    }

    post("/verify") {
        if (call.request.contentType().match(ContentType.MultiPart.FormData).not()) {
            return@post respondVerifyPage(VerifyState.NoInput, null, status = HttpStatusCode.BadRequest)
        }

        var input: String? = null
        var sha256: String? = null
        var tooLarge = false
        // Every part is read (or discarded), even after an error: see discardRemaining() in MultipartRequests.kt.
        // Ktor's limit applies to files too: they're streamed here instead, and capped at MAX_UPLOAD_SIZE below.
        call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
            try {
                when {
                    part is PartData.FormItem && part.name == "code" -> input = part.value
                    part is PartData.FileItem && part.name == "file" -> {
                        val digest = MessageDigest.getInstance("SHA-256")
                        val channel = part.provider()
                        val buffer = ByteArray(64 * 1024)
                        var size = 0L
                        var empty = true
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read == -1) break
                            if (read > 0) empty = false
                            size += read
                            if (size <= MAX_UPLOAD_SIZE) digest.update(buffer, 0, read)
                        }
                        // A form submitted without choosing a file still sends an empty part
                        if (!empty) {
                            if (size > MAX_UPLOAD_SIZE) tooLarge = true else sha256 = digest.digest().toHex()
                        }
                    }
                    part is PartData.FileItem -> {
                        val channel = part.provider()
                        val buffer = ByteArray(64 * 1024)
                        while (channel.readAvailable(buffer, 0, buffer.size) != -1) Unit
                    }
                }
            } finally {
                part.dispose()
            }
        }

        if (tooLarge) return@post respondVerifyPage(VerifyState.TooLarge, input, status = HttpStatusCode.PayloadTooLarge)
        val hash = sha256
        if (input.isNullOrBlank()) {
            // Just the file: whichever document it is
            if (hash == null) return@post respondVerifyPage(VerifyState.NoInput, null, status = HttpStatusCode.BadRequest)
            val document = Database { DocumentVerification.findBySha256(hash) }.firstOrNull()
                ?: return@post respondVerifyPage(VerifyState.UnknownFile, null, status = HttpStatusCode.NotFound)
            return@post respondVerifyPage(VerifyState.Match, document.code, document)
        }

        val code = DocumentVerification.normalize(input!!)
            ?: return@post respondVerifyPage(VerifyState.InvalidCode, input, status = HttpStatusCode.BadRequest)
        val document = Database { DocumentVerification.find(code) }
            ?: return@post respondVerifyPage(VerifyState.NotFound, input, status = HttpStatusCode.NotFound)
        when {
            hash == null -> respondVerifyPage(VerifyState.Found, document.code, document)
            hash == document.sha256 -> respondVerifyPage(VerifyState.Match, document.code, document)
            // Not the document's details: the file isn't it
            else -> respondVerifyPage(VerifyState.Mismatch, document.code, status = HttpStatusCode.Conflict)
        }
    }
}
