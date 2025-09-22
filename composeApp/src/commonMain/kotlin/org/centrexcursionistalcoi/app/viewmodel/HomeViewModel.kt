package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.network.getHttpClient

class HomeViewModel: ViewModel() {
    private val httpClient = getHttpClient()

    fun loadDepartments() = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            val departments = httpClient.get("/departments").let {
                val raw = it.bodyAsText()
                json.decodeFromString(ListSerializer(Department.serializer()), raw)
            }
            Napier.i { "Loaded ${departments.size} departments" }
        }
    }

    fun createDepartment(name: String) = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            try {
                val response = httpClient.submitFormWithBinaryData(
                    url = "/departments",
                    formData = formData {
                        append("displayName", name)
                    }
                )
                val departmentLocation = response.headers[HttpHeaders.Location]
                Napier.i { "Created department: $departmentLocation" }
            } catch (e: Exception) {
                Napier.e("Error creating department", e)
            }
        }
    }
}
