package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,
    users: List<UserData>?,
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(key = "departments") {
            DepartmentsCard(departments, onCreateDepartment, onDeleteDepartment)
        }
        item(key = "users") {
            UsersCard(users)
        }
    }
}

@Composable
private fun <T> ListCard(
    list: List<T>?,
    titleResource: StringResource,
    emptyTextResource: StringResource,
    displayName: (T) -> String,
    modifier: Modifier = Modifier,
    onCreate: (() -> Unit)? = null,
    onDelete: ((T) -> Job)? = null,
) {
    var deleting by remember { mutableStateOf<T?>(null) }
    if (onDelete != null) {
        deleting?.let { item ->
            DeleteDialog(
                item,
                { it.toString() },
                { onDelete(item) }
            ) { deleting = null }
        }
    }

    OutlinedCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(titleResource),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            onCreate?.let { create ->
                IconButton(
                    onClick = create
                ) { Icon(Icons.Default.Add, stringResource(Res.string.create)) }
            }
        }
        if (list == null) {
            Text(stringResource(Res.string.status_loading))
        } else if (list.isEmpty()) {
            Text(stringResource(emptyTextResource))
        } else {
            for (item in list) {
                ListItem(
                    headlineContent = { Text(displayName(item)) },
                    trailingContent = if (onDelete != null) {
                        {
                            IconButton(
                                onClick = { deleting = item }
                            ) {
                                Icon(Icons.Default.Delete, stringResource(Res.string.delete))
                            }
                        }
                    } else null,
                    leadingContent = {
                        if (item is Department && item.imageFile != null) {
                            val imageFile by item.rememberImageFile()
                            AsyncImage(
                                model = imageFile,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDelete: (Department) -> Job,
) {
    var deleting by remember { mutableStateOf<Department?>(null) }
    deleting?.let { department ->
        DeleteDialog(department, { it.displayName }, { onDelete(department) }) { deleting = null }
    }

    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateDepartmentDialog(onCreate) { creating = false }
    }

    ListCard(
        list = departments,
        titleResource = Res.string.management_departments,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.displayName },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onDelete = onDelete
    )
}

@Composable
fun UsersCard(users: List<UserData>?) {
    ListCard(
        list = users,
        titleResource = Res.string.management_users,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.username },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

@Composable
fun CreateDepartmentDialog(
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image
    ) { file -> image = file }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create department") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                    onClick = { imagePicker.launch() }
                ) {
                    image?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    } ?: Text(
                        "Select image (optional)",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName, image).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}
