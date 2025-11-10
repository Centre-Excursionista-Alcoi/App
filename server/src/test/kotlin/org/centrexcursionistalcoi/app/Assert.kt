package org.centrexcursionistalcoi.app

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.test.assertEquals
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.error.Error

/**
 * Asserts that two JSON strings are equivalent, ignoring formatting and key order.
 * Also ignores types, so `"1"` is equal to `1`.
 *
 * Optionally, a set of keys to ignore during comparison can be provided (only applies for objects).
 */
fun assertJsonEquals(expected: String, actual: String, ignoreKeys: Set<String> = emptySet()) {
    val suffix = "\n\tActual: $actual\n\tExpected: $expected"
    val expectedJson = json.parseToJsonElement(expected)
    val actualJson = json.parseToJsonElement(actual)

    try {
        val expectedPrimitive = expectedJson.jsonPrimitive
        try {
            actualJson.jsonPrimitive
        } catch (_: IllegalArgumentException) {
            throw AssertionError("Expected JSON primitive $expectedPrimitive but was $actualJson")
        }
        // both are jsonPrimitives, compare their string values
        assertEquals(expectedPrimitive.content, actualJson.jsonPrimitive.content)
        return
    } catch (_: IllegalArgumentException) {
        // ignore, element is not a jsonPrimitive
    }

    try {
        val expectedObject = expectedJson.jsonObject.filterKeys { !ignoreKeys.contains(it) }
        val actualObject = actualJson.jsonObject.filterKeys { !ignoreKeys.contains(it) }
        assertEquals(expectedObject.size, actualObject.size, "JSON objects have different number of keys.$suffix")
        for ((key, expectedValue) in expectedObject) {
            val actualValue = actualObject[key]
                ?: throw AssertionError("Expected key '$key' not found in actual JSON object.$suffix")
            assertJsonEquals(expectedValue.toString(), actualValue.toString())
        }
        return
    } catch (_: IllegalArgumentException) {
        // ignore, element is not a jsonObject
    }

    try {
        val expectedArray = expectedJson.jsonArray
        val actualArray = actualJson.jsonArray
        assertEquals(expectedArray.size, actualArray.size, "JSON arrays have different lengths.$suffix")
        for (i in expectedArray.indices) {
            assertJsonEquals(expectedArray[i].toString(), actualArray[i].toString())
        }
        return
    } catch (_: IllegalArgumentException) {
        // ignore, element is not a jsonArray
    }

    throw AssertionError("Could not compare JSON elements: $expectedJson and $actualJson")
}

fun HttpResponse.assertSuccess() {
    assert(status.isSuccess()) { "Expected a successful status code (200-299)." }
}

suspend fun HttpResponse.assertStatusCode(expected: HttpStatusCode) {
    assert(expected == status) {
        "Expected $expected response. Got: $status. Body: ${bodyAsText()}"
    }
}


suspend fun HttpResponse.assertError(expected: Error) {
    assert(expected.statusCode == status) {
        "Expected $expected response. Got: $status. Body: ${bodyAsText()}"
    }
    headers["CEA-Error-Code"]?.let { errorCodeHeader ->
        assert(expected.code.toString() == errorCodeHeader) {
            "Expected error code ${expected.code}. Got: $errorCodeHeader. Body: ${bodyAsText()}"
        }
    } ?: throw AssertionError("Expected error code header not found. Body: ${bodyAsText()}")
}

suspend fun HttpResponse.assertBadRequest() {
    assertStatusCode(HttpStatusCode.BadRequest)
}

suspend fun HttpResponse.assertBody(block: suspend (body: String) -> Unit) {
    val body = bodyAsText()
    block(body)
}

suspend fun <T> HttpResponse.assertBody(serializer: DeserializationStrategy<T>, block: suspend (body: T) -> Unit) {
    val body = bodyAsText()
    println("Decoding JSON: $body")
    val json = json.decodeFromString(serializer, body)
    block(json)
}
