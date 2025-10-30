package org.centrexcursionistalcoi.app.database.entity

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.days
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json

class TestSpaceEntity {
    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.init(TEST_URL)

        val dbEntity = Database {
            SpaceEntity.new {
                name = "Conference Room"
                description = "A spacious conference room."
                price = 6.toBigDecimal()
                priceDuration = Duration.ofDays(1)
                capacity = 50
            }
        }
        val sharedEntity = Space(
            id = dbEntity.id.value.toKotlinUuid(),
            name = "Conference Room",
            description = "A spacious conference room.",
            price = Pair(6.0, 1.days),
            capacity = 50,
        )

        assertJsonEquals(
            json.encodeEntityToString(dbEntity),
            json.encodeToString(Space.serializer(), sharedEntity)
        )
    }

    @Test
    fun `test not allowed to set price without duration`() = runTest {
        Database.init(TEST_URL)

        assertFails {
            Database {
                SpaceEntity.new {
                    name = "Conference Room"
                    price = 6.toBigDecimal()
                }
            }
        }
        assertFails {
            Database {
                SpaceEntity.new {
                    name = "Conference Room"
                    priceDuration = Duration.ofDays(1)
                }
            }
        }
    }
}
