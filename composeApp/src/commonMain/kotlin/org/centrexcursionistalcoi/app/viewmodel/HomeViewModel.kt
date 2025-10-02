package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.storage.databaseInstance

class HomeViewModel: ViewModel() {
    private val httpClient = getHttpClient()

    val departments = DepartmentsRepository.selectAllAsFlow()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun loadDepartments() = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            val departments = httpClient.get("/departments").let {
                val raw = it.bodyAsText()
                json.decodeFromString(ListSerializer(Department.serializer()), raw)
            }
            Napier.i { "Loaded ${departments.size} departments" }

            val dbDepartments = DepartmentsRepository.selectAll()
            val addedIds = mutableListOf<Long>()
            for (department in departments) {
                if (dbDepartments.find { it.id == department.id } != null) {
                    DepartmentsRepository.update(department)
                } else {
                    DepartmentsRepository.insert(department)
                }
                addedIds += department.id
            }
            // Delete departments that are not in the server response
            DepartmentsRepository.deleteByIdList(
                dbDepartments.filter { it.id !in addedIds }.map { it.id }
            )
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

                loadDepartments()
            } catch (e: Exception) {
                Napier.e("Error creating department", e)
            }
        }
    }
}
