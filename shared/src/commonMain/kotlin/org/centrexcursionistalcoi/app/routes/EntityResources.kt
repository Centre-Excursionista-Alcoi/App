package org.centrexcursionistalcoi.app.routes

import kotlinx.serialization.KSerializer

/**
 * The resources of an entity served by the server's generic CRUD routes (`provideEntityRoutes`), and synchronized by
 * the app's `RemoteRepository`: `GET`/`POST` on [collection], and `GET`/`PATCH`/`DELETE` on [item].
 *
 * Ids are kept as strings, so that a malformed one still reaches the route and is answered with the API's own error.
 */
class EntityResources<C : Any, I : Any>(
    val collectionSerializer: KSerializer<C>,
    val itemSerializer: KSerializer<I>,
    val collection: C,
    val item: (id: String) -> I,
    val idOf: (I) -> String,
)
