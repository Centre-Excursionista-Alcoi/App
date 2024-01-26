package network

import io.ktor.client.HttpClient

/**
 * The platform-specific Ktor http client.
 */
expect val client: HttpClient
