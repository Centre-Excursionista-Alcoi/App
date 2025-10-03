package org.centrexcursionistalcoi.app

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Asserts that two JSON strings are equivalent, ignoring formatting and key order.
 * Also ignores types, so `"1"` is equal to `1`.
 */
fun assertJsonEquals(expected: String, actual: String) {
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
        val expectedObject = expectedJson.jsonObject
        val actualObject = actualJson.jsonObject
        assertEquals(expectedObject.size, actualObject.size, "JSON objects have different number of keys")
        for ((key, expectedValue) in expectedObject) {
            val actualValue = actualObject[key]
                ?: throw AssertionError("Expected key '$key' not found in actual JSON object")
            assertJsonEquals(expectedValue.toString(), actualValue.toString())
        }
        return
    } catch (_: IllegalArgumentException) {
        // ignore, element is not a jsonObject
    }

    try {
        val expectedArray = expectedJson.jsonArray
        val actualArray = actualJson.jsonArray
        assertEquals(expectedArray.size, actualArray.size, "JSON arrays have different lengths")
        for (i in expectedArray.indices) {
            assertJsonEquals(expectedArray[i].toString(), actualArray[i].toString())
        }
        return
    } catch (_: IllegalArgumentException) {
        // ignore, element is not a jsonArray
    }

    throw AssertionError("Could not compare JSON elements: $expectedJson and $actualJson")
}

suspend fun HttpResponse.assertStatusCode(expected: HttpStatusCode) {
    assert(expected == status) {
        "Expected $expected response. Got: $status. Body: ${bodyAsText()}"
    }
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
    val json = json.decodeFromString(serializer, body)
    block(json)
}
