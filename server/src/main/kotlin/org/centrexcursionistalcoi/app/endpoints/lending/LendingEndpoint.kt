package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.getBooking

object LendingEndpoint : SecureEndpoint("/lending/{id}", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        getBooking(user, Lending, ItemLendingD.serializer())
    }
}
