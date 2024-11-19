package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.ItemLendingD

object LendingsEndpoint: SecureEndpoint("/lendings", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val query = call.request.queryParameters
        val all = query["all"]?.toBoolean() ?: false

        val lendings = ServerDatabase {
            if (all && user.isAdmin) {
                Lending.all()
            } else {
                Lending.find { LendingsTable.user eq user.id }
            }.map(Lending::serializable)
        }
        respondSuccess(
            data = lendings,
            serializer = ListSerializer(ItemLendingD.serializer())
        )
    }
}
