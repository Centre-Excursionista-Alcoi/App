package network

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

actual val client: HttpClient = HttpClient(Java)
