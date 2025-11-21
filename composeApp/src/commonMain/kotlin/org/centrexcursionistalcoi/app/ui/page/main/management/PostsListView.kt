package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.localizedDate
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.editor.RichTextStyleRow
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsListView(
    windowSizeClass: WindowSizeClass,
    posts: List<ReferencedPost>?,
    departments: List<Department>?,
    onCreate: (title: String, department: Department?, content: RichTextState, link: String, files: List<PlatformFile>, progressNotifier: (Progress) -> Unit) -> Job,
    onUpdate: (postId: Uuid, title: String?, department: Department?, content: RichTextState?, link: String?, removedFiles: List<Uuid>, files: List<PlatformFile>, progressNotifier: (Progress) -> Unit) -> Job,
    onDelete: (ReferencedPost) -> Job,
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
        onDeleteRequest = onDelete,
        editItemContent = { post ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }

            var title by remember { mutableStateOf(post?.title ?: "") }
            var department by remember { mutableStateOf(post?.department) }
            val content = rememberRichTextState()
            var link by remember { mutableStateOf(post?.link ?: "") }
            var removedFiles by remember { mutableStateOf(emptyList<Uuid>()) }
            var newFiles by remember { mutableStateOf(listOf<PlatformFile>()) }

            LaunchedEffect(post) {
                if (post != null) {
                    content.setMarkdown(post.content)
                }
            }

            val isDirty =
                post == null || title != post.title || department?.id != post.department?.id || content.toMarkdown() != post.content || link != post.link || removedFiles.isNotEmpty() || newFiles.isNotEmpty()

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
                label = stringResource(Res.string.post_department).optional(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                itemToString = { it?.displayName ?: stringResource(Res.string.post_department_generic) },
                allowNull = true,
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text(stringResource(Res.string.post_link)) },
                placeholder = { Text("https://...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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

            val filePicker = rememberFilePickerLauncher(FileKitType.Image, FileKitMode.Multiple()) { pickedFiles ->
                if (pickedFiles.isNullOrEmpty()) return@rememberFilePickerLauncher
                newFiles += pickedFiles
            }
            OutlinedButton(
                enabled = !isLoading,
                onClick = { filePicker.launch() },
            ) {
                Icon(Icons.Default.FileUpload, stringResource(Res.string.post_upload_images))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.post_upload_images))
            }
            if (newFiles.isNotEmpty()) {
                LazyRow(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(newFiles) { file ->
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (post == null) {
                        onCreate(title, department, content, link, newFiles) {
                            progress = it
                        }
                    } else {
                        onUpdate(
                            post.id,
                            title.takeIf { it != post.title },
                            department.takeIf { it?.id != post.department?.id },
                            content.takeIf { it.toMarkdown() != post.content },
                            link.takeIf { it != post.link },
                            removedFiles,
                            newFiles,
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
        val uriHandler = LocalUriHandler.current

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
        post.link?.let { link ->
            Row(
                modifier = Modifier.clickable { uriHandler.openUri(link) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, null, tint = Color(0xff267ae8))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = link,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xff267ae8), textDecoration = TextDecoration.Underline)
                )
            }
        }

        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
