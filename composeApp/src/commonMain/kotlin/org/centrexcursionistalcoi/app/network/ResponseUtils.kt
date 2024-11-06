package org.centrexcursionistalcoi.app.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.util.date.GMTDateParser

/**
 * Parse the cookies from the response headers
 * @return The list of cookies
 */
fun HttpResponse.cookies(): List<Cookie> {
    val cookies = headers.getAll("Set-Cookie") ?: return emptyList()
    val pairs = cookies.map { cookieStr ->
        cookieStr.split(";")
            .map(String::trim)
            .map { it.split("=") }
            .associate { it[0] to if (it.isNotEmpty()) it.subList(1, it.size).joinToString("=") else "" }
    }
    // value is the first pair, the rest are attributes
    return pairs.map { map ->
        Cookie(
            name = map.keys.first(),
            value = map.values.first(),
            maxAge = map["Max-Age"]?.toInt(),
            // Example: Wed, 04 Dec 2024 11:58:00 GMT
            expires = map["Expires"]?.let {
                GMTDateParser(pattern = "dd MMM YYYY hh:mm:ss",).parse(it.substring(5, 25))
            },
            path = map["Path"],
            httpOnly = map.containsKey("HttpOnly"),
            secure = map.containsKey("Secure"),
            domain = map["Domain"],
            encoding = map["\$x-enc"]?.let(CookieEncoding::valueOf) ?: CookieEncoding.URI_ENCODING,
            extensions = map.toList()
                .let { it.subList(1, it.size) }
                .toMap()
                .filterKeys { it !in setOf("Max-Age", "Expires", "Path", "HttpOnly", "Secure", "Domain", "\$x-enc") }
        )
    }
}
