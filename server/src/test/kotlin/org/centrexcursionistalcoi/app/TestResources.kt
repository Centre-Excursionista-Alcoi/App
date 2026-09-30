package org.centrexcursionistalcoi.app

import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import io.ktor.resources.serialization.ResourcesFormat
import org.centrexcursionistalcoi.app.routes.EntityResources
import io.ktor.resources.href as resourceHref

/** The path (and query) of [resource], for requests Ktor has no resource-based builder for (like `submitForm`). */
inline fun <reified T : Any> href(resource: T): String = resourceHref(ResourcesFormat(), resource)

/** The path of the collection of these resources. */
fun <C : Any, I : Any> EntityResources<C, I>.collectionHref(): String = href(collectionSerializer, collection)

/** The path of the item [id] of these resources. */
fun <C : Any, I : Any> EntityResources<C, I>.itemHref(id: Any): String = href(itemSerializer, item(id.toString()))

private fun <T> href(serializer: kotlinx.serialization.KSerializer<T>, resource: T): String {
    val urlBuilder = URLBuilder()
    resourceHref(ResourcesFormat(), serializer, resource, urlBuilder)
    return urlBuilder.build().fullPath
}
