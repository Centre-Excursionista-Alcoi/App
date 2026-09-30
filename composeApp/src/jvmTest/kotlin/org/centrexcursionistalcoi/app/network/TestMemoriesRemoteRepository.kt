package org.centrexcursionistalcoi.app.network

import androidx.room3.Room
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A synced memory whose submitter isn't stored locally (a non-admin can't list other users) can be stored, but not
 * read back as a [org.centrexcursionistalcoi.app.data.ReferencedMemory] until the
 * [org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier] backfills that user. Syncing must still succeed
 * instead of failing on the read back.
 */
class TestMemoriesRemoteRepository {
    private val original = _httpClient
    private var db: AppDatabase? = null

    @BeforeTest
    fun setUp() {
        // RemoteRepository stores the last synchronization time in the settings.
        startKoin { modules(module { single { mockk<SettingsStore>(relaxed = true) } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        _httpClient = original
        db?.close()
    }

    private val memory = Memory(
        id = Uuid.random(),
        place = null,
        members = emptyList(),
        externalUsers = null,
        text = "Memory text",
        sport = null,
        department = null,
        attachments = emptyList(),
        submittedBy = "not-synced-locally",
        from = ZonedDateTime.fromInstant(Instant.fromEpochMilliseconds(0), TimeZone.UTC),
        to = ZonedDateTime.fromInstant(Instant.fromEpochMilliseconds(0), TimeZone.UTC),
        pdf = null,
        lending = null,
    )

    @Test
    fun `synchronizing a memory whose submitter is missing locally stores it without failing`() = runTest {
        _httpClient = HttpClient(MockEngine {
            respond(
                json.encodeToString(ListSerializer(Memory.serializer()), listOf(memory)),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(Resources) }
        val database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>(), Dispatchers.IO)
        db = database
        val repository = MemoriesRemoteRepository(MemoriesRepository(database))

        repository.synchronizeWithDatabase(ignoreIfModifiedSince = true)

        val stored = database.memoryDao().get(memory.id)
        assertNotNull(stored, "the memory must be stored for the integrity verifier to repair")
        assertEquals(memory.submittedBy, stored.memory.submittedBy)
    }
}
