package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.serialization.bodyAsJson

class TestInfo : ApplicationTestBase() {
    @Test
    fun `test info endpoint`() = runApplicationTest {
        try {
            mockkObject(ConfigEntity.DatabaseVersion)
            every { ConfigEntity.DatabaseVersion.get() } returns 123
            mockkObject(ConfigEntity.LastCEASync)
            every { ConfigEntity.LastCEASync.get() } returns Instant.ofEpochSecond(1763531703)

            val response = client.get("/info")
            assertTrue(response.status.isSuccess())
            val body = response.bodyAsJson(ServerInfo.serializer())
            assertEquals(123, body.databaseVersion)
            assertEquals(1763531703000L, body.lastCEASync)
        } finally {
            unmockkObject(ConfigEntity.DatabaseVersion, ConfigEntity.LastCEASync)
        }
    }
}
