package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ApplicationTestBase.LoginType
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.serialization.list
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.assertInstanceOf
import org.jetbrains.exposed.v1.dao.Entity as ExposedEntity

object ProvidedRouteTests {
    private suspend fun HttpClient.request(url: String, method: HttpMethod, contentType: ContentType?, expectedStatusCode: HttpStatusCode) {
        request(url) {
            this.method = method
            if (contentType != null) contentType(contentType)
        }.apply {
            assertStatusCode(expectedStatusCode)
        }
    }

    context(base: ApplicationTestBase)
    fun test_notLoggedIn(baseUrl: String, method: HttpMethod = HttpMethod.Get, contentType: ContentType? = null) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        client.request(baseUrl, method, contentType, HttpStatusCode.Unauthorized)
    }

    context(base: ApplicationTestBase)
    fun test_notLoggedIn_form(baseUrl: String) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        client.submitFormWithBinaryData(baseUrl, emptyList()).apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
        }
    }

    context(base: ApplicationTestBase)
    fun test_loggedIn_notAdmin(baseUrl: String, method: HttpMethod = HttpMethod.Get, contentType: ContentType? = null) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) { loginAsFakeUser() }

        client.request(baseUrl, method, contentType, HttpStatusCode.Forbidden)
    }

    context(base: ApplicationTestBase)
    fun test_loggedIn_notAdmin_form(baseUrl: String) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) { loginAsFakeUser() }

        client.submitFormWithBinaryData(baseUrl, listOf()).apply {
            assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    context(base: ApplicationTestBase)
    fun <T> test_loggedIn(
        baseUrl: String,
        deserializer: DeserializationStrategy<T>,
        isAdmin: Boolean = false,
        block: suspend (T) -> Unit
    ) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) {
            if (isAdmin) loginAsFakeAdminUser()
            else loginAsFakeUser()
        }

        client.get(baseUrl).apply {
            assertStatusCode(HttpStatusCode.OK)
            val response = bodyAsJson(deserializer)
            block(response)
        }
    }

    data class TestCase(
        val name: String,
        val block: ApplicationTestBase.() -> Unit,
        val skip: Boolean = false,
        val before: (() -> Unit)? = null,
        val after: (() -> Unit)? = null
    ) {
        infix fun skipIf(condition: Boolean): TestCase {
            return this.copy(skip = condition)
        }

        infix fun before(block: () -> Unit): TestCase {
            return this.copy(before = block)
        }

        infix fun after(block: () -> Unit): TestCase {
            return this.copy(after = block)
        }
    }

    infix fun String.runs(block: ApplicationTestBase.() -> Unit): TestCase {
        return TestCase(this, block)
    }

    private fun Collection<KCallable<*>>.call(name: String, vararg args: Any?): Any? {
        val callable = first { it.name == name }
        return try {
            println("Calling $callable.\n  Arguments: ${args.joinToString()}")
            callable.call(*args)
        } catch (e: InvocationTargetException) {
            if (e.cause is IllegalStateException) {
                // Retry inside a transaction
                Database { callable.call(*args) }
            } else {
                throw e
            }
        }
    }

    private fun formDataOf(pairs: List<Pair<String, () -> Any?>>) = formData {
        println("Constructing form data:")
        for ((key, valueProvider) in pairs) {
            when (val value = valueProvider()) {
                is String -> append(key, value).also { println("- $key: $value") }
                is ByteArray -> append(
                    key,
                    value,
                    Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"raw-bytes\"")
                    }
                ).also { println("- $key: byte array (size=${value.size})") }
                is Number -> append(key, value).also { println("- $key: $value") }
                is Boolean -> append(key, value).also { println("- $key: $value") }
                null -> {}
                else -> append(key, value.toString()).also { println("- $key: $value") }
            }
        }
    }

    context(_: JdbcTransaction)
    private fun Any.populate(name: String, valueProvider: () -> Any) {
        val memberProperty = this::class.memberProperties.firstOrNull { it.name == name }
        assertNotNull(memberProperty) { "Could not find \"$name\" in properties. List: ${this::class.memberProperties.joinToString()}" }
        assertInstanceOf<KMutableProperty<*>>(memberProperty, "Property $name is not mutable: $memberProperty")
        val member = memberProperty.setter
        val value = valueProvider()
        try {
            when (value) {
                is String -> member.call(this, value)
                is Number -> member.call(this, value)
                is Boolean -> member.call(this, value)
                is ByteArray -> {
                    // For ByteArray, the setter is a FileEntity. We have to create a FileEntity first.
                    val fileEntity = transaction {
                        FileEntity.new {
                            this.name = "bytes"
                            this.type = "application/octet-stream"
                            this.data = value
                        }
                    }
                    member.call(this, fileEntity)
                }
                else -> error("Type of $name not supported: ${value::class}")
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("argument type mismatch") == true) {
                throw IllegalArgumentException(
                    "Type of $name not supported.\nThis: ${this::class.simpleName}\nValue: ${value::class.simpleName}\nMember: $member",
                    e
                )
            } else {
                throw e
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    context(base: ApplicationTestBase)
    fun <EID: Any, EE: ExposedEntity<EID>, TID: Any, ET: Entity<TID>> runTestsOnRoute(
        title: String,
        baseUrl: String,
        listLoginType: LoginType = LoginType.USER,
        creationLoginType: LoginType = LoginType.ADMIN,
        requiredCreationValues: Map<String, () -> Any>,
        optionalCreationValues: Map<String, () -> Any> = emptyMap(),
        locationRegex: Regex,
        entityClass: EntityClass<EID, EE>,
        idTypeConverter: (String) -> EID,

        /**
         * Converts the internal ID type [TID] to the exposed ID type [EID].
         *
         * For example, if the internal ID is [java.util.UUID] and the exposed ID is [kotlin.uuid.Uuid],
         * this function should convert [java.util.UUID] to [kotlin.uuid.Uuid].
         */
        exposedIdTypeConverter: (TID) -> EID,
        dataEntitySerializer: KSerializer<ET>
    ): List<DynamicTest> {
        return listOf(
            "$title - Test fetching list when not logged in" runs {
                test_notLoggedIn(baseUrl)
            } skipIf (listLoginType != LoginType.ADMIN),
            "$title - Test fetching list" runs {
                Database.init(TEST_URL) // initialize the database

                // Insert some data into the database to be fetched
                val entities = mutableListOf<EE>()
                // Insert one without optional values
                Database {
                    entities += entityClass.new {
                        for ((name, valueProvider) in requiredCreationValues) populate(name, valueProvider)
                    }
                }
                // One with each optional value
                for ((name, valueProvider) in optionalCreationValues) {
                    Database {
                        entities += entityClass.new {
                            // Fill all required values
                            for ((rName, rValueProvider) in requiredCreationValues) populate(rName, rValueProvider)
                            // Fill the optional value
                            populate(name, valueProvider)
                        }
                    }
                }
                // And one with all optional values
                Database {
                    entities += entityClass.new {
                        for ((name, valueProvider) in requiredCreationValues) populate(name, valueProvider)
                        for ((name, valueProvider) in optionalCreationValues) populate(name, valueProvider)
                    }
                }

                runApplicationTest(shouldLogIn = LoginType.USER) {
                    client.get(baseUrl).apply {
                        assertStatusCode(HttpStatusCode.OK)
                        assertBody(dataEntitySerializer.list()) { list ->
                            val receivedIds = list.map { it.id.let(exposedIdTypeConverter) }.toSet()
                            val expectedIds = entities.map { it.id.value }.toSet()
                            assertEquals(expectedIds, receivedIds, "Received IDs do not match expected IDs")
                        }
                    }
                }
            },

            "$title - Test create when not logged in" runs {
                test_notLoggedIn_form(baseUrl)
            },
            "$title - Test create not admin" runs {
                test_loggedIn_notAdmin_form(baseUrl)
            } skipIf (creationLoginType != LoginType.ADMIN),
            "$title - Test create with invalid content type" runs {
                runApplicationTest(shouldLogIn = creationLoginType) {
                    client.post(baseUrl).apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            "$title - Test create without data" runs {
                runApplicationTest(shouldLogIn = creationLoginType) {
                    client.submitFormWithBinaryData(baseUrl, formData = listOf()).apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            // Generate tests for each required field
            *requiredCreationValues.map { (name) ->
                "$title - Test create without $name" runs {
                    runApplicationTest(shouldLogIn = creationLoginType) {
                        val data = formDataOf(requiredCreationValues.toList().filter { it.first != name })
                        client.submitFormWithBinaryData(baseUrl, formData = data).apply {
                            assertStatusCode(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }.toTypedArray(),
            "$title - Test creation with required parameters (${requiredCreationValues.keys.joinToString()})" runs {
                runApplicationTest(shouldLogIn = creationLoginType) {
                    val data = formDataOf(requiredCreationValues.toList())
                    val location = client.submitFormWithBinaryData(baseUrl, formData = data).run {
                        assertStatusCode(HttpStatusCode.Created)
                        val location = headers[HttpHeaders.Location]
                        assertNotNull(location)
                        assertTrue { location.matches(locationRegex) }
                        location
                    }
                    val id = location.substringAfterLast('/').let(idTypeConverter)
                    Database { entityClass.findById(id) }.let { entity ->
                        assertNotNull(entity)

                        // Get a list of all the fields of the entity
                        val members = entity::class.members
                        val names = members.map { it.name }.toSet()

                        // Check that all required fields are present
                        for ((name) in requiredCreationValues) {
                            assertTrue { name in names }
                            val expected = requiredCreationValues[name]?.invoke()
                            val actual = members.call(name, entity)
                            assertEquals(expected, actual, "Field $name does not match")
                        }
                        // Check that all optional fields are not present
                        for ((name) in optionalCreationValues) {
                            assertTrue { name in names }
                            val actual = members.call(name, entity)
                            assertNull(actual, "Field $name should not be present")
                        }
                    }
                    client.get(location).apply {
                        assertStatusCode(HttpStatusCode.OK)
                        assertBody(dataEntitySerializer) { entity ->
                            assertEquals(id, entity.id.let(exposedIdTypeConverter))

                            // Get a list of all the fields of the entity
                            val members = entity::class.members
                            val fields = members.map { it.name }.toSet()

                            // Check that all required fields are present
                            for ((name) in requiredCreationValues) {
                                assertTrue { name in fields }
                                val expected = requiredCreationValues[name]?.invoke()
                                val actual = members.first { it.name == name }.call(entity)
                                assertEquals(expected, actual, "Field $name does not match")
                            }
                            // Check that all optional fields are not present
                            for ((name) in optionalCreationValues) {
                                assertNull(
                                    members.firstOrNull { it.name == name }?.call(entity),
                                    "Field $name should not be present"
                                )
                            }
                        }
                    }
                }
            },
            *optionalCreationValues.map { (name) ->
                "$title - Test create with optional parameter \"$name\"" runs {
                    runApplicationTest(shouldLogIn = creationLoginType) {
                        val data = formDataOf(
                            requiredCreationValues.toList() + (name to optionalCreationValues[name]!!)
                        )
                        val location = client.submitFormWithBinaryData(baseUrl, formData = data).run {
                            assertStatusCode(HttpStatusCode.Created)
                            val location = headers[HttpHeaders.Location]
                            assertNotNull(location)
                            assertTrue { location.matches(locationRegex) }
                            location
                        }
                        val id = location.substringAfterLast('/').let(idTypeConverter)
                        Database { entityClass.findById(id) }.let { entity ->
                            assertNotNull(entity)

                            // Get a list of all the fields of the entity
                            val members = entity::class.members
                            val names = members.map { it.name }.toSet()

                            // Check that all required fields are present
                            for ((name) in requiredCreationValues) {
                                assertTrue { name in names }
                                val expected = requiredCreationValues[name]?.invoke()
                                val actual = members.call(name, entity)
                                assertEquals(expected, actual, "Field $name does not match")
                            }
                            // Check that the field is present
                            assertTrue { name in names }
                            val expected = optionalCreationValues[name]!!.invoke()
                            println("Expected value for $name: $expected (${expected::class.simpleName})")
                            val actual = members.call(name, entity)
                            assertNotNull(actual, "Field $name should be present")
                            if (actual is FileEntity) {
                                assertInstanceOf<ByteArray>(expected, "Expected value for $name should be a ByteArray")
                                val actualBytes = Database { actual.data }
                                assertContentEquals(expected, actualBytes, "Field $name contents does not match")
                            } else {
                                assertEquals(expected, actual, "Field $name does not match")
                            }
                        }
                        client.get(location).apply {
                            assertStatusCode(HttpStatusCode.OK)
                            assertBody(dataEntitySerializer) { entity ->
                                assertEquals(id, entity.id.let(exposedIdTypeConverter))

                                // Get a list of all the fields of the entity
                                val members = entity::class.members
                                val fields = members.map { it.name }.toSet()

                                // Check that all required fields are present
                                for ((name) in requiredCreationValues) {
                                    assertTrue { name in fields }
                                    val expected = requiredCreationValues[name]?.invoke()
                                    val actual = members.call(name, entity)
                                    assertEquals(expected, actual, "Field $name does not match")
                                }

                                // Check that the field is present and correct
                                assertTrue { name in fields }
                                val expected = optionalCreationValues[name]?.invoke()
                                val actual = members.call(name, entity)
                                if (expected is ByteArray) {
                                    // If expected is a ByteArray, actual is an UUID matching a FileEntity.
                                    // Fetch the FileEntity from the database and compare its data.
                                    assertInstanceOf<Uuid>(actual, "Expected value for $name should be a Uuid")
                                    val fileEntity = Database { FileEntity.findById(actual.toJavaUuid()) }
                                    assertNotNull(fileEntity, "FileEntity with id $actual not found in database")
                                    val actualBytes = Database { fileEntity.data }
                                    assertContentEquals(expected, actualBytes, "Field $name contents does not match")
                                } else {
                                    assertEquals(expected, actual, "Field $name does not match")
                                }
                            }
                        }
                    }
                }
            }.toTypedArray()
        ).mapNotNull { (name, block, skip, before, after) ->
            if (skip) return@mapNotNull null
            DynamicTest.dynamicTest(name) {
                try {
                    before?.invoke()
                    block(base)
                } finally {
                    after?.invoke()
                }
            }
        }
    }
}
