package org.centrexcursionistalcoi.app.routes

import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TestInfo : ApplicationTestBase() {
    @Test
    fun `test info endpoint`() = runApplicationTest {
        try {
            mockkObject(ConfigEntity.DatabaseVersion)
            every { ConfigEntity.DatabaseVersion.get() } returns 123
            mockkObject(ConfigEntity.LastCEASync)
            every { ConfigEntity.LastCEASync.get() } returns Instant.fromEpochSeconds(1763531703)
            mockkObject(ConfigEntity.LastFEMECVSync)
            every { ConfigEntity.LastFEMECVSync.get() } returns Instant.fromEpochSeconds(1763531703)

            val response = client.get(Api.Info())
            assertTrue(response.status.isSuccess())
            val body = response.bodyAsJson(ServerInfo.serializer())
            assertEquals(123, body.version.databaseVersion)
            assertEquals(1763531703000L, body.lastCEASync)
            assertEquals(1763531703000L, body.lastFEMECVSync)
        } finally {
            unmockkObject(ConfigEntity.DatabaseVersion, ConfigEntity.LastCEASync, ConfigEntity.LastFEMECVSync)
        }
    }
}
