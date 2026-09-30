package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.resources.href
import org.centrexcursionistalcoi.app.routes.EntityResources

/** Points the request at the collection of [resources]. */
fun <C : Any, I : Any> HttpRequestBuilder.collection(client: HttpClient, resources: EntityResources<C, I>) {
    href(client.plugin(Resources).resourcesFormat, resources.collectionSerializer, resources.collection, url)
}

/** Points the request at the item [id] of [resources]. */
fun <C : Any, I : Any> HttpRequestBuilder.item(client: HttpClient, resources: EntityResources<C, I>, id: String) {
    href(client.plugin(Resources).resourcesFormat, resources.itemSerializer, resources.item(id), url)
}

/** Points the request at [resource], for requests Ktor has no resource-based builder for (like `submitForm`). */
inline fun <reified T : Any> HttpRequestBuilder.resource(client: HttpClient, resource: T) {
    href(client.plugin(Resources).resourcesFormat, resource, url)
}
