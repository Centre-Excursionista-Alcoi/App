package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.listBookings

object LendingsEndpoint: SecureEndpoint("/lendings", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        listBookings(
            user = user,
            table = LendingsTable,
            entityClass = Lending,
            serializer = ItemLendingD.serializer()
        )
    }
}
