package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.localizedDate
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsListView(
    windowSizeClass: WindowSizeClass,
    posts: List<ReferencedPost>?,
    departments: List<Department>?,
    onCreate: (title: String, department: Department?, content: RichTextState, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdate: (postId: Uuid, title: String?, department: Department?, content: RichTextState?, progressNotifier: (Progress) -> Unit) -> Job,
) {
    ListView(
        windowSizeClass = windowSizeClass,
        items = posts,
        itemIdProvider = { it.id },
        itemDisplayName = { it.title },
        itemSupportingContent = { Text(it.localizedDate()) },
        emptyItemsText = stringResource(Res.string.management_no_posts),
        isCreatingSupported = true,
        createTitle = stringResource(Res.string.management_post_create),
        editItemContent = { post ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }

            var title by remember { mutableStateOf(post?.title ?: "") }
            var department by remember { mutableStateOf(post?.department) }
            val content = rememberRichTextState()

            val isDirty = post == null || title != post.title || department?.id != post.department?.id || content.toMarkdown() != post.content

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.post_title)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            DropdownField(
                value = department,
                onValueChange = { department = it },
                options = departments.orEmpty(),
                label = stringResource(Res.string.post_department),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                itemToString = { it?.displayName ?: stringResource(Res.string.post_department_generic) },
                allowNull = true,
            )

            RichTextStyleRow(
                modifier = Modifier.fillMaxWidth(),
                state = content,
                enabled = !isLoading,
            )
            OutlinedRichTextEditor(
                state = content,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (post == null) {
                        onCreate(title, department, content) {
                            progress = it
                        }
                    } else {
                        onUpdate(
                            post.id,
                            title.takeIf { it != post.title },
                            department.takeIf { it?.id != post.department?.id },
                            content.takeIf { it.toMarkdown() != post.content },
                        ) {
                            progress = it
                        }
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { post ->
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Row {
            Text(
                text = stringResource(Res.string.post_by, post.department?.displayName ?: stringResource(Res.string.post_department_generic)),
            )
            Text(" - ")
            Text(
                text = post.localizedDate(),
            )
        }

        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
