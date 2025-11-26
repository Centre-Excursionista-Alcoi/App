package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.lang.reflect.InvocationTargetException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.Temporal
import java.util.Random
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.toJavaInstant
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.ifModifiedSinceFormatter
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.test.TestCase.Companion.runs
import org.centrexcursionistalcoi.app.test.TestCase.Companion.withEntities
import org.centrexcursionistalcoi.app.utils.Zero
import org.centrexcursionistalcoi.app.utils.toJsonElement
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.assertInstanceOf
import kotlin.time.Instant as KotlinInstant
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.LocalTime as KotlinLocalTime
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
            if (isAdmin) {
                Database { FakeAdminUser.provideEntity() }
                loginAsFakeAdminUser()
            } else {
                Database { FakeUser.provideEntity() }
                loginAsFakeUser()
            }
        }

        client.get(baseUrl).apply {
            assertStatusCode(HttpStatusCode.OK)
            val response = bodyAsJson(deserializer)
            block(response)
        }
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

    private fun formDataOf(pairs: List<Pair<String, Any?>>) = formData {
        println("Constructing form data:")
        for ((key, value) in pairs) {
            when (value) {
                is String -> append(key, value).also { println("- $key: $value") }
                is ByteArray -> append(key, Base64.UrlSafe.encode(value).also { println("- $key: $it") })
                is FileWithContext -> append(
                    key,
                    value.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, (value.contentType ?: ContentType.Application.OctetStream).toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"${value.name ?: "raw-bytes"}\"")
                    }
                ).also { println("- $key: byte array (size=${value.bytes.size})") }
                is Number -> append(key, value).also { println("- $key: $value") }
                is Boolean -> append(key, value).also { println("- $key: $value") }
                is Instant -> append(key, value.toEpochMilli()).also { println("- $key: $value") }
                null -> {}
                else -> append(key, value.toString()).also { println("- $key: $value") }
            }
        }
    }

    context(_: JdbcTransaction)
    private fun Any.populate(name: String, value: Any, foreignTypesAssociations: Map<String, EntityClass<*, *>>) {
        val memberProperty = this::class.memberProperties.firstOrNull { it.name == name }
        assertNotNull(memberProperty, "Could not find \"$name\" in properties. List: ${this::class.memberProperties.joinToString()}")
        assertInstanceOf<KMutableProperty<*>>(memberProperty, "Property $name is not mutable: $memberProperty")
        val member = memberProperty.setter
        try {
            when (value) {
                is String -> member.call(this, value)
                is Number -> member.call(this, value)
                is Boolean -> member.call(this, value)
                is ByteArray -> member.call(this, value)
                is Temporal -> member.call(this, value)
                is FileWithContext -> {
                    // For FileBytesWrapper, the setter is a FileEntity. We have to create a FileEntity first.
                    val fileEntity = transaction {
                        FileEntity.new {
                            this.name = value.name ?: "bytes"
                            this.type = (value.contentType ?: ContentType.Application.OctetStream).toString()
                            this.bytes = value.bytes
                        }
                    }
                    member.call(this, fileEntity)
                }
                is UUID -> @Suppress("UNCHECKED_CAST") if (this is ExposedEntity<*>) {
                    this as ExposedEntity<UUID>

                    val foreignEntityClass = foreignTypesAssociations[name] as EntityClass<UUID, ExposedEntity<UUID>>?
                    checkNotNull(foreignEntityClass) { "Could not find a foreign type association for key \"$name\"" }

                    val entity = foreignEntityClass.findById(value)
                    checkNotNull(entity) { "Could not find a foreign type association for key \"$name\"" }
                    member.call(this, entity)
                } else {
                    member.call(this, value)
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
    context(_: ApplicationTestBase)
    fun <EE: UUIDEntity, ET: Entity<Uuid>> runTestsOnRoute(
        title: String,
        baseUrl: String,
        now: java.time.Instant? = null,

        listLoginType: LoginType = LoginType.USER,
        /**
         * If the given [listLoginType] variates the entities that are listed, this filter can be used
         * to filter them accordingly.
         */
        filterEntitiesForListLoginType: JdbcTransaction.(EE) -> Boolean = { true },
        modificationsLoginType: LoginType = LoginType.ADMIN,

        /**
         * Patches to apply to the user entity after creation.
         *
         * Applies to all [listLoginType] and [modificationsLoginType].
         */
        userEntityPatches: JdbcTransaction.(UserReferenceEntity) -> Unit = {},

        requiredCreationValuesProvider: Map<String, () -> Any>,
        optionalCreationValuesProvider: Map<String, () -> Any> = emptyMap(),
        defaultCreationValuesProvider: Map<String, DefaultValue> = emptyMap(),

        locationRegex: Regex,
        entityClass: UUIDEntityClass<EE>,

        /**
         * A provider that creates an example entity to be used in some endpoints that require an entity instance.
         *
         * It's guaranteed that this will only be called once per test run.
         *
         * Make sure only the entries in [requiredCreationValuesProvider] are populated, as the tests will
         * check that optional values are not present unless explicitly specified.
         */
        stubEntityProvider: JdbcTransaction.() -> EE,

        /**
         * If this test requires some auxiliary entities (foreign keys, etc.), they can be created here.
         */
        auxiliaryEntitiesProvider: JdbcTransaction.() -> Unit = {},

        /**
         * If the entity being tested has foreign key references to other entities, they must be specified here.
         *
         * This is a map that relates the name of the property in the serializable entity to the [EntityClass] of the referenced entity.
         */
        foreignTypesAssociations: Map<String, EntityClass<*, *>> = emptyMap(),

        seed: Long = 0,

        dataEntitySerializer: KSerializer<ET>
    ): List<DynamicTest> = runTestsOnRoute(
        title = title,
        baseUrl = baseUrl,
        now = now,
        listLoginType = listLoginType,
        filterEntitiesForListLoginType = filterEntitiesForListLoginType,
        modificationsLoginType = modificationsLoginType,
        userEntityPatches = userEntityPatches,
        requiredCreationValuesProvider = requiredCreationValuesProvider,
        optionalCreationValuesProvider = optionalCreationValuesProvider,
        defaultCreationValuesProvider = defaultCreationValuesProvider,
        locationRegex = locationRegex,
        entityClass = entityClass,
        idTypeConverter = { it.toUUID() },
        exposedIdTypeConverter = { it.toJavaUuid() },
        stubEntityProvider = stubEntityProvider,
        invalidEntityId = Uuid.Zero.toJavaUuid(),
        auxiliaryEntitiesProvider = auxiliaryEntitiesProvider,
        foreignTypesAssociations = foreignTypesAssociations,
        seed = seed,
        dataEntitySerializer = dataEntitySerializer
    )

    @OptIn(InternalSerializationApi::class)
    context(_: ApplicationTestBase)
    fun <EID: Comparable<EID>, EE: ExposedEntity<EID>, TID: Comparable<TID>, ET: Entity<TID>> runTestsOnRoute(
        title: String,
        baseUrl: String,
        now: java.time.Instant? = null,

        listLoginType: LoginType = LoginType.USER,
        /**
         * If the given [listLoginType] variates the entities that are listed, this filter can be used
         * to filter them accordingly.
         */
        filterEntitiesForListLoginType: JdbcTransaction.(EE) -> Boolean = { true },
        modificationsLoginType: LoginType = LoginType.ADMIN,

        /**
         * Patches to apply to the user entity after creation.
         *
         * Applies to all [listLoginType] and [modificationsLoginType].
         */
        userEntityPatches: JdbcTransaction.(UserReferenceEntity) -> Unit = {},

        requiredCreationValuesProvider: Map<String, () -> Any>,
        optionalCreationValuesProvider: Map<String, () -> Any> = emptyMap(),
        defaultCreationValuesProvider: Map<String, DefaultValue> = emptyMap(),

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

        /**
         * A provider that creates an example entity to be used in some endpoints that require an entity instance.
         *
         * It's guaranteed that this will only be called once per test run.
         *
         * Make sure only the entries in [requiredCreationValues] are populated, as the tests will
         * check that optional values are not present unless explicitly specified.
         */
        stubEntityProvider: JdbcTransaction.() -> EE,

        /**
         * An ID that is guaranteed to not exist in the database.
         *
         * Used in tests that require a non-existing entity ID.
         */
        invalidEntityId: EID,

        /**
         * If this test requires some auxiliary entities (foreign keys, etc.), they can be created here.
         */
        auxiliaryEntitiesProvider: JdbcTransaction.() -> Unit = {},

        /**
         * If the entity being tested has foreign key references to other entities, they must be specified here.
         *
         * This is a map that relates the name of the property in the serializable entity to the [EntityClass] of the referenced entity.
         */
        foreignTypesAssociations: Map<String, EntityClass<*, *>> = emptyMap(),

        seed: Long = 0,

        dataEntitySerializer: KSerializer<ET>
    ): List<DynamicTest> {
        val seededRandom = Random(seed)

        fun provideRequiredCreationValues(): Map<String, Any> = requiredCreationValuesProvider.mapValues { (_, provider) -> provider() }
        fun provideOptionalCreationValues(): Map<String, Any> = optionalCreationValuesProvider.mapValues { (_, provider) -> provider() }
        fun provideDefaultCreationValues(): Map<String, ProvidedDefaultValue> = defaultCreationValuesProvider.mapValues { (_, provider) -> provider(seededRandom) }

        fun fetchFromDatabaseAndCheckFields(
            id: EID,
            presentValues: Map<String, Any> = emptyMap(),
            absentValues: Map<String, Any> = emptyMap(),
            checkSpecificFieldPresent: Pair<String, Any>? = null,
        ) {
            Database { entityClass.findById(id) }.let { entity ->
                assertNotNull(entity)

                // Get a list of all the fields of the entity
                val members = entity::class.members
                val names = members.map { it.name }.toSet()

                fun <T> T.assertActual(name: String, expected: T) {
                    when (this) {
                        is FileEntity -> {
                            assertInstanceOf<FileWithContext>(expected, "Expected value for $name should be a FileBytesWrapper")
                            val actualBytes = Database { bytes }
                            assertContentEquals(expected.bytes, actualBytes, "Field $name contents does not match")
                        }

                        is ExposedEntity<*> -> {
                            // If the actual value is an ExposedEntity, the expected value is a UUID matching its ID.
                            assertInstanceOf<UUID>(expected, "Expected value for $name should be a UUID")
                            val actualId = this.id.value
                            assertEquals(expected, actualId, "Field $name ID does not match")
                        }

                        is ByteArray -> {
                            assertInstanceOf<ByteArray>(expected, "Expected value for $name should be a ByteArray")
                            assertContentEquals(expected, this, "Field $name contents does not match")
                        }

                        else -> {
                            assertEquals(expected, this, "Field $name does not match")
                        }
                    }
                }

                // Check that all required fields are present
                for ((name) in presentValues) {
                    assertTrue { name in names }
                    val expected = presentValues[name]
                    val actual = members.call(name, entity)
                    actual.assertActual(name, expected)
                }
                // Check that all optional fields are not present
                for ((name) in absentValues) {
                    assertTrue { name in names }
                    val actual = members.call(name, entity)
                    assertNull(actual, "Field $name should not be present")
                }
                // Check that the specific field is present
                checkSpecificFieldPresent?.let { (name, expected) ->
                    assertTrue { name in names }
                    println("Expected value for $name: $expected (${expected::class.simpleName})")
                    val actual = members.call(name, entity)
                    assertNotNull(actual, "Field $name should be present")
                    actual.assertActual(name, expected)
                }
            }
        }
        suspend fun ApplicationTestBuilder.fetchFromServerAndCheckFields(
            url: String,
            id: EID,
            presentValues: Map<String, Any> = emptyMap(),
            absentValues: Map<String, Any> = emptyMap(),
            checkSpecificFieldPresent: Pair<String, Any>? = null,
        ) {
            client.get(url).apply {
                assertStatusCode(HttpStatusCode.OK)
                assertBody(dataEntitySerializer) { entity ->
                    assertEquals(id, entity.id.let(exposedIdTypeConverter))

                    // Get a list of all the fields of the entity
                    val members = entity::class.members
                    val fields = members.map { it.name }.toSet()

                    fun Any.assertActual(name: String, expected: Any?) {
                        val actual = this
                        if (expected is FileWithContext) {
                            // If expected is a FileBytesWrapper, actual is a UUID matching a FileEntity.
                            // Fetch the FileEntity from the database and compare its data.
                            assertInstanceOf<Uuid>(actual, "Expected value for $name should be a Uuid")
                            val fileEntity = Database { FileEntity.findById(actual.toJavaUuid()) }
                            assertNotNull(fileEntity, "FileEntity with id $actual not found in database")
                            val actualBytes = Database { fileEntity.bytes }
                            assertContentEquals(expected.bytes, actualBytes, "Field $name contents does not match")
                        } else if (expected is UUID && actual is Uuid) {
                            // If expected is a UUID and actual is a Uuid, compare their values
                            assertEquals(expected, actual.toJavaUuid(), "Field $name ID does not match")
                        } else if (expected is ByteArray) {
                            assertInstanceOf<ByteArray>(actual, "Expected value for $name should be a ByteArray")
                            assertContentEquals(expected, actual, "Field $name contents does not match")
                        } else if (expected is LocalDate && actual is KotlinLocalDate) {
                            assertEquals(expected, actual.toJavaLocalDate(), "Date $name does not match")
                        } else if (expected is LocalTime && actual is KotlinLocalTime) {
                            assertEquals(expected, actual.toJavaLocalTime(), "Time $name does not match")
                        } else if (expected is Instant && actual is KotlinInstant) {
                            assertEquals(expected, actual.toJavaInstant(), "Instant $name does not match")
                        } else {
                            assertEquals(expected, actual, "Field $name does not match")
                        }
                    }

                    // Check that all required fields are present
                    for ((name, expected) in presentValues) {
                        assertTrue("\"$name\" is not defined for ${entity::class.simpleName}. Fields: $fields") { name in fields }
                        val actual = members.call(name, entity)
                        assertNotNull(actual, "Field $name should be present")
                        actual.assertActual(name, expected)
                    }

                    // Check that all optional fields are not present
                    for ((name) in absentValues) {
                        assertTrue("\"$name\" is not defined for ${entity::class.simpleName}. Fields: $fields") { name in fields }
                        val actual = members.call(name, entity)
                        assertNull(actual, "Field $name should not be present")
                    }

                    // Check that the field is present and correct
                    checkSpecificFieldPresent?.let { (name, expected) ->
                        assertTrue("\"$name\" is not defined for ${entity::class.simpleName}. Fields: $fields") { name in fields }
                        val actual = members.call(name, entity)
                        println("Name: $name")
                        println("Entity: $entity")
                        println("Members: $members")
                        println("Actual value for $name: $actual (${actual?.let { it::class.simpleName }})")
                        assertNotNull(actual, "Field $name should be present")
                        actual.assertActual(name, expected)
                    }
                }
            }
        }

        return listOf(
            "$title - Test fetching list when not logged in" runs {
                test_notLoggedIn(baseUrl)
            } skipIf (listLoginType != LoginType.ADMIN),
            "$title - Test fetching list" withEntities auxiliaryEntitiesProvider runs {
                Database.init(TEST_URL) // initialize the database
                Push.disable = true     // disable push notifications

                // Insert some data into the database to be fetched
                val entities = mutableListOf<EE>()
                // Insert one without optional values
                Database {
                    entities += entityClass.new {
                        for ((name, valueProvider) in provideRequiredCreationValues()) populate(name, valueProvider, foreignTypesAssociations)
                        // Also fill default values
                        for ((name, valueProvider) in provideDefaultCreationValues()) populate(name, valueProvider.default, foreignTypesAssociations)
                    }
                }
                // One with each optional value
                for ((name, valueProvider) in provideOptionalCreationValues()) {
                    Database {
                        entities += entityClass.new {
                            // Fill all required values
                            for ((rName, rValueProvider) in provideRequiredCreationValues()) populate(rName, rValueProvider, foreignTypesAssociations)
                            // Fill the optional value
                            populate(name, valueProvider, foreignTypesAssociations)
                            // Also fill default values
                            for ((dName, dValueProvider) in provideDefaultCreationValues()) populate(dName, dValueProvider.default, foreignTypesAssociations)
                        }
                    }
                }
                // One with each default value
                for ((name, valueProvider) in provideDefaultCreationValues()) {
                    Database {
                        entities += entityClass.new {
                            // Fill all required values
                            for ((rName, rValueProvider) in provideRequiredCreationValues()) populate(rName, rValueProvider, foreignTypesAssociations)
                            // Fill the provided default value
                            populate(name, valueProvider.value, foreignTypesAssociations)
                        }
                    }
                }
                // And one with all optional and provided default values
                Database {
                    entities += entityClass.new {
                        for ((name, valueProvider) in provideRequiredCreationValues()) populate(name, valueProvider, foreignTypesAssociations)
                        for ((name, valueProvider) in provideOptionalCreationValues()) populate(name, valueProvider, foreignTypesAssociations)
                        for ((name, valueProvider) in provideDefaultCreationValues()) populate(name, valueProvider.value, foreignTypesAssociations)
                    }
                }

                runApplicationTest(shouldLogIn = LoginType.USER, userEntityPatches = userEntityPatches) {
                    val entityClass = entities.first()::class
                    val isLastUpdateEntity = entityClass.supertypes
                        .map { it.toString() }
                        .find { it.endsWith("LastUpdateEntity") } != null
                    if (isLastUpdateEntity) {
                        client.get(baseUrl) {
                            val time = now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC).toLocalDateTime()
                            headers.append(HttpHeaders.IfModifiedSince, ifModifiedSinceFormatter.format(time))
                        }.apply {
                            assertStatusCode(HttpStatusCode.NotModified)
                        }
                    } else {
                        error("Entity (${entityClass.simpleName}) is not a LastUpdateEntity.")
                    }

                    client.get(baseUrl).apply {
                        assertStatusCode(HttpStatusCode.OK)
                        assertBody(dataEntitySerializer.list()) { list ->
                            val expectedIds = Database {
                                entities
                                    .filter { filterEntitiesForListLoginType(it) }
                                    .map { it.id.value }
                            }
                            val actualIds = list.map { it.id.let(exposedIdTypeConverter) }
                            assertContentEquals(
                                expectedIds,
                                actualIds,
                                "Received IDs do not match expected IDs.\n\tReceived IDs: ${actualIds}\n\tExpected IDs: $expectedIds"
                            )
                        }
                    }
                }
            } after {
                // Re-enable push notifications
                Push.disable = false
            },

            "$title - Test create when not logged in" runs {
                test_notLoggedIn_form(baseUrl)
            },
            "$title - Test create not admin" runs {
                test_loggedIn_notAdmin_form(baseUrl)
            } skipIf (modificationsLoginType != LoginType.ADMIN),
            "$title - Test create with invalid content type" runs {
                runApplicationTest(shouldLogIn = modificationsLoginType) {
                    client.post(baseUrl).apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            "$title - Test create without data" runs {
                runApplicationTest(shouldLogIn = modificationsLoginType) {
                    client.submitFormWithBinaryData(baseUrl, formData = listOf()).apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            // Generate tests for each required field
            *provideRequiredCreationValues().let { requiredCreationValues ->
                requiredCreationValues.map { (name) ->
                    "$title - Test create without $name" runs {
                        runApplicationTest(shouldLogIn = modificationsLoginType) {
                            val data = formDataOf(requiredCreationValues.toList().filter { it.first != name })
                            client.submitFormWithBinaryData(baseUrl, formData = data).apply {
                                assertStatusCode(HttpStatusCode.BadRequest)
                            }
                        }
                    }
                }
            }.toTypedArray(),
            (provideRequiredCreationValues() to provideOptionalCreationValues()).let { (requiredCreationValues, optionalCreationValues) ->
                "$title - Test creation with required parameters (${requiredCreationValues.keys.joinToString()})" withEntities auxiliaryEntitiesProvider runs {
                    runApplicationTest(shouldLogIn = modificationsLoginType, userEntityPatches = userEntityPatches) {
                        val data = formDataOf(requiredCreationValues.toList())
                        val location = client.submitFormWithBinaryData(baseUrl, formData = data).run {
                            assertStatusCode(HttpStatusCode.Created)
                            val location = headers[HttpHeaders.Location]
                            assertNotNull(location)
                            assertTrue { location.matches(locationRegex) }
                            location
                        }
                        val id = location.substringAfterLast('/').let(idTypeConverter)
                        fetchFromDatabaseAndCheckFields(id, requiredCreationValues, optionalCreationValues)
                        fetchFromServerAndCheckFields(
                            url = location,
                            id = id,
                            presentValues = requiredCreationValues,
                            absentValues = optionalCreationValues
                        )
                    }
                }
            },
            *(provideRequiredCreationValues() to provideOptionalCreationValues()).let { (requiredCreationValues, optionalCreationValues) ->
                optionalCreationValues.map { (name) ->
                    "$title - Test create with optional parameter \"$name\"" withEntities auxiliaryEntitiesProvider runs {
                        runApplicationTest(shouldLogIn = modificationsLoginType, userEntityPatches = userEntityPatches) {
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
                            fetchFromDatabaseAndCheckFields(
                                id,
                                requiredCreationValues,
                                checkSpecificFieldPresent = name to optionalCreationValues[name]!!,
                            )
                            fetchFromServerAndCheckFields(
                                url = location,
                                id = id,
                                presentValues = requiredCreationValues,
                                checkSpecificFieldPresent = name to optionalCreationValues[name]!!,
                            )
                        }
                    }
                }
            }.toTypedArray(),

            "$title - Test patch when not logged in" runs {
                test_notLoggedIn("$baseUrl/$invalidEntityId", HttpMethod.Patch, ContentType.Application.Json)
            },
            "$title - Test patch not admin" runs {
                test_loggedIn_notAdmin("$baseUrl/$invalidEntityId", HttpMethod.Patch, ContentType.Application.Json)
            } skipIf (modificationsLoginType != LoginType.ADMIN),
            "$title - Test patch with invalid content type" runs {
                runApplicationTest(shouldLogIn = modificationsLoginType) {
                    client.patch("$baseUrl/$invalidEntityId").apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            "$title - Test patch on unknown entity" runs {
                runApplicationTest(shouldLogIn = modificationsLoginType) {
                    client.patch("$baseUrl/$invalidEntityId") {
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }.apply {
                        assertStatusCode(HttpStatusCode.NotFound)
                    }
                }
            },
            "$title - Test patch without data" withEntities auxiliaryEntitiesProvider withEntity stubEntityProvider runs {
                runApplicationTest(shouldLogIn = modificationsLoginType) {
                    client.patch(baseUrl.withEntityId()) {
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }.apply {
                        assertStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            },
            // Generate tests for each required field
            // TODO: should requests for patching fail when required arguments are not given? This test should not be necessary then.
            /**(provideRequiredCreationValues() to provideOptionalCreationValues()).let { (requiredCreationValues, optionalCreationValues) ->
                requiredCreationValues.map { (name) ->
                    "$title - Test patch without $name" withEntities auxiliaryEntitiesProvider withEntity stubEntityProvider runs {
                        runApplicationTest(shouldLogIn = modificationsLoginType, userEntityPatches = userEntityPatches) {
                            val obj = JsonObject(
                                requiredCreationValues
                                    .filter { it.key != name }
                                    .map { (name, value) -> name to value.toJsonElement() }
                                    .toMap()
                            )
                            client.patch(baseUrl.withEntityId()) {
                                contentType(ContentType.Application.Json)
                                setBody(json.encodeToString(obj))
                            }.apply {
                                assertStatusCode(HttpStatusCode.BadRequest)
                            }
                        }
                    }
                }
            }.toTypedArray(),*/
            *(provideRequiredCreationValues() to provideOptionalCreationValues()).let { (requiredCreationValues, optionalCreationValues) ->
                val creationValues = requiredCreationValues + optionalCreationValues
                creationValues.map { entry ->
                    val (name, value) = entry
                    "$title - Test patch $name" withEntities auxiliaryEntitiesProvider withEntity stubEntityProvider runs {
                        runApplicationTest(shouldLogIn = modificationsLoginType, userEntityPatches = userEntityPatches) {
                            val data = JsonObject(
                                mapOf(name to value.toJsonElement())
                            )
                            val location = client.patch(baseUrl.withEntityId()) {
                                contentType(ContentType.Application.Json)
                                setBody(json.encodeToString(data))
                            }.run {
                                assertStatusCode(HttpStatusCode.OK)
                                val location = headers[HttpHeaders.Location]
                                assertNotNull(location)
                                assertTrue { location.matches(locationRegex) }
                                location
                            }
                            val id = location.substringAfterLast('/').let(idTypeConverter)
                            fetchFromDatabaseAndCheckFields(
                                id,
                                requiredCreationValues + entry.toPair(),
                                optionalCreationValues - entry.key
                            )
                            fetchFromServerAndCheckFields(
                                url = location,
                                id = id,
                                presentValues = requiredCreationValues + entry.toPair(),
                                absentValues = optionalCreationValues - entry.key
                            )
                        }
                    }
                }
            }.toTypedArray(),
            (provideRequiredCreationValues() to provideOptionalCreationValues()).let { (requiredCreationValues, optionalCreationValues) ->
                val creationValues = requiredCreationValues + optionalCreationValues
                "$title - Test patch all parameters (${creationValues.keys.joinToString()})" withEntities auxiliaryEntitiesProvider withEntity stubEntityProvider runs {
                    runApplicationTest(shouldLogIn = modificationsLoginType, userEntityPatches = userEntityPatches) {
                        val data = JsonObject(
                            creationValues.map { (name, value) -> name to value.toJsonElement() }.toMap()
                        )
                        val location = client.patch(baseUrl.withEntityId()) {
                            contentType(ContentType.Application.Json)
                            setBody(json.encodeToString(data))
                        }.run {
                            assertStatusCode(HttpStatusCode.OK)
                            val location = headers[HttpHeaders.Location]
                            assertNotNull(location)
                            assertTrue { location.matches(locationRegex) }
                            location
                        }
                        val id = location.substringAfterLast('/').let(idTypeConverter)
                        fetchFromDatabaseAndCheckFields(id, creationValues)
                        fetchFromServerAndCheckFields(
                            url = location,
                            id = id,
                            presentValues = creationValues
                        )
                    }
                }
            },
        ).mapNotNull { it.createDynamicTest(at = now) }
    }

    class DefaultValue(
        val default: Any,
        val provider: (seededRandom: Random) -> Any,
    ) {
        operator fun invoke(seededRandom: Random): ProvidedDefaultValue = ProvidedDefaultValue(default, provider(seededRandom))
    }

    class ProvidedDefaultValue(
        val default: Any,
        val value: Any,
    )
}
