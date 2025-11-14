package org.centrexcursionistalcoi.app.exception

/**
 * Exception thrown when a resource has not been modified since last retrieval.
 *
 * It's expected to be handled appropriately by the caller, typically by using cached data.
 */
class ResourceNotModifiedException : RuntimeException("This resource has not been modified since last retrieval")
