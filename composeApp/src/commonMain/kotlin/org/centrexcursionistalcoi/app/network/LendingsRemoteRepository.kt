package org.centrexcursionistalcoi.app.network

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.json

object LendingsRemoteRepository : RemoteRepository<Uuid, Lending>(
    "/inventory/lendings",
    Lending.serializer(),
    LendingsRepository
) {
    suspend fun create(from: LocalDate, to: LocalDate, itemsIds: List<Uuid>, notes: String? = null) {
        val response = httpClient.submitForm("inventory/lendings", parameters {
            append("from", from.toString())
            append("to", to.toString())
            append("items", itemsIds.joinToString(","))
            if (notes != null) {
                append("notes", notes)
            }
        })
        if (response.status.isSuccess()) {
            val location = response.headers["Location"]
                ?: throw IllegalArgumentException("Missing Location header in response")
            val id = location.substringAfterLast("/").let { Uuid.parse(it) }
            val lending = get(id) ?: throw NoSuchElementException("Lending $id not found after creation")
            LendingsRepository.insert(lending)
        } else {
            throw IllegalArgumentException("Failed to create lending (${response.status}): ${response.bodyAsText()}")
        }
    }

    /**
     * Requests the server for available items of a given type within a date range, and allocates the specified amount if available.
     * @param typeId The UUID of the inventory item type to allocate.
     * @param from The start date of the allocation period.
     * @param to The end date of the allocation period.
     * @param amount The number of items to allocate.
     * @return A list of UUIDs representing the allocated inventory items.
     * @throws IllegalStateException if there are not enough items available to allocate.
     * @throws IllegalArgumentException for other allocation failures.
     */
    suspend fun allocate(typeId: Uuid, from: LocalDate, to: LocalDate, amount: Int): List<Uuid> {
        require(amount > 0) { "Amount must be greater than zero" }

        val response = httpClient.get("inventory/types/$typeId/allocate") {
            parameter("from", from.toString())
            parameter("to", to.toString())
            parameter("amount", amount)
        }
        if (response.status.isSuccess()) {
            return response.bodyAsText().let { body ->
                json.decodeFromString(ListSerializer(Uuid.serializer()), body)
            }
        } else {
            when (response.status) {
                HttpStatusCode.Conflict -> {
                    throw IllegalStateException("Not enough items available to allocate")
                }
                else -> {
                    throw IllegalArgumentException("Failed to allocate items (${response.status}): ${response.bodyAsText()}")
                }
            }
        }
    }
}
