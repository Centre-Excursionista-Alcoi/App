package org.centrexcursionistalcoi.app.database.base

import org.centrexcursionistalcoi.app.request.UpdateEntityRequest
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface EntityPatcher<R : UpdateEntityRequest<*, *>> {
    context(_: JdbcTransaction)
    fun patch(request: R)
}
