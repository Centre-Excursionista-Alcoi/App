package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class PostsManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    postsRepository: PostsRepository,
    private val postsRemoteRepository: PostsRemoteRepository,
) : ViewModel() {
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val posts = postsRepository.selectAllAsFlow().stateInViewModel()

    fun createPost(
        title: String,
        department: Department?,
        content: RichTextState,
        link: String,
        files: List<PlatformFile>,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        withContext(dispatcherProvider.io) {
            val contentMarkdown = content.toMarkdown()

            postsRemoteRepository.create(
                title,
                contentMarkdown,
                department?.id,
                link.takeUnless { it.isBlank() },
                files,
                progressNotifier
            )
        }
    }

    fun updatePost(
        postId: Uuid,
        title: String?,
        department: Department?,
        content: RichTextState?,
        link: String?,
        removedFiles: List<Uuid>,
        files: List<PlatformFile>,
        progressNotifier: (Progress) -> Unit
    ) = launch {
        withContext(dispatcherProvider.io) {
            val contentMarkdown = content?.toMarkdown()

            postsRemoteRepository.update(
                postId,
                title,
                contentMarkdown,
                department?.id,
                link,
                files,
                removedFiles,
                progressNotifier
            )
        }
    }

    fun delete(post: ReferencedPost) = launch {
        withContext(dispatcherProvider.io) {
            postsRemoteRepository.delete(post.id)
        }
    }
}
