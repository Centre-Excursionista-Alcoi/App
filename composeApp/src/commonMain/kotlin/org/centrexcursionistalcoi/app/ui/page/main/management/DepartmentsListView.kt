package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.ui.reusable.form.ReadOnlyFormField
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentsListView(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onUpdate: (id: Uuid, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDelete: (Department) -> Job,
) {
    var deleting by remember { mutableStateOf<Department?>(null) }
    deleting?.let { department ->
        DeleteDialog(
            item = department,
            displayName = { it.displayName },
            onDelete = { onDelete(department) },
            onDismissRequested = { deleting = null }
        )
    }

    ListView(
        windowSizeClass = windowSizeClass,
        items = departments,
        itemIdProvider = { it.id },
        itemDisplayName = { it.displayName },
        itemLeadingContent = { department ->
            department.image ?: return@ListView
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        emptyItemsText = stringResource(Res.string.management_no_departments),
        itemToolbarActions = {
            TooltipIconButton(
                imageVector = Icons.Default.Delete,
                tooltip = stringResource(Res.string.delete),
                onClick = { deleting = it }
            )
        },
        createTitle = stringResource(Res.string.management_department_create),
        editItemContent = { department: Department? ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }
            var displayName by remember { mutableStateOf(department?.displayName ?: "") }
            var image by remember { mutableStateOf<PlatformFile?>(null) }

            val isDirty = if (department == null) true else displayName != department.displayName || image != null

            FormImagePicker(
                image = image,
                container = department,
                onImagePicked = { image = it },
                isLoading = isLoading,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.form_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (department == null) {
                        onCreate(displayName, image) {
                            progress = it
                        }
                    } else {
                        onUpdate(department.id, displayName, image) {
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
    ) { department ->
        if (department.image != null) {
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        ReadOnlyFormField(
            value = department.displayName,
            label = stringResource(Res.string.form_display_name),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
