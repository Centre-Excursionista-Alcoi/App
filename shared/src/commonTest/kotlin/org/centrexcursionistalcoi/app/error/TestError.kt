package org.centrexcursionistalcoi.app.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.json

class TestError {
    @Test
    fun `test decode`() {
        json.decodeFromString(
            ErrorPolymorphicSerializer,
            "{\"code\":1,\"description\":\"Not logged in\",\"statusCode\":401}"
        ).let { decodedError ->
            assertEquals(1, decodedError.code)
            assertEquals("Not logged in", decodedError.description)
            assertEquals(401, decodedError.statusCode.value)
        }

        json.decodeFromString(
            ErrorPolymorphicSerializer,
            "{\"code\":0,\"description\":\"Internal Server Exception: example\",\"message\":\"example\",\"statusCode\":500}"
        ).let { decodedError ->
            assertEquals(0, decodedError.code)
            assertEquals("Internal Server Exception: example", decodedError.description)
            assertEquals(500, decodedError.statusCode.value)

            decodedError as Error.Unknown
            assertEquals("example", decodedError.message)
        }
    }

    @Test
    fun `test encode`() {
        json.encodeToString(Error.serializer(), Error.Unknown("example")).let { string ->
            json.decodeFromString(JsonObject.serializer(), string).let { obj ->
                assertEquals(0, obj["code"]!!.jsonPrimitive.int)
                assertEquals("Internal Server Exception: example", obj["description"]!!.jsonPrimitive.content)
                assertEquals(500, obj["statusCode"]!!.jsonPrimitive.int)

                assertEquals("example", obj["message"]!!.jsonPrimitive.content)
            }
        }
        json.encodeToString(Error.serializer(), Error.NotLoggedIn()).let { string ->
            json.decodeFromString(JsonObject.serializer(), string).let { obj ->
                assertEquals(1, obj["code"]?.jsonPrimitive?.int ?: error("Could not find \"code\" in: $string"))
                assertEquals("Not logged in", obj["description"]?.jsonPrimitive?.content ?: error("Could not find \"description\" in: $string"))
                assertEquals(401, obj["statusCode"]?.jsonPrimitive?.int ?: error("Could not find \"statusCode\" in: $string"))
            }
        }
    }
}
