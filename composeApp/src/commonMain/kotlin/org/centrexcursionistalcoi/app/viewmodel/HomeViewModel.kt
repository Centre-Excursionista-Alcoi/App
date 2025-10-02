package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository

class HomeViewModel: ViewModel() {
    val departments = DepartmentsRepository.selectAllAsFlow()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val posts = PostsRepository.selectAllAsFlow()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun loadDepartments() = viewModelScope.launch(defaultAsyncDispatcher) {
        DepartmentsRemoteRepository.synchronizeWithDatabase()
    }

    fun loadPosts() = viewModelScope.launch(defaultAsyncDispatcher) {
        PostsRemoteRepository.synchronizeWithDatabase()
    }

    fun createDepartment(name: String) = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            try {
                DepartmentsRemoteRepository.create(name)

                loadDepartments()
            } catch (e: Exception) {
                Napier.e("Error creating department", e)
            }
        }
    }

    fun createPost(
        title: String,
        content: String,
        onlyForMembers: Boolean,
        department: Department
    ) = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            try {
                PostsRemoteRepository.create(title, content, onlyForMembers, department.id)

                loadPosts()
            } catch (e: Exception) {
                Napier.e("Error creating post", e)
            }
        }
    }
}
