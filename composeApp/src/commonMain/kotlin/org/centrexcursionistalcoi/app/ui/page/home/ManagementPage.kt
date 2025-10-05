package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?,
    onCreateDepartment: (displayName: String) -> Job,
    onDeleteDepartment: (Department) -> Job
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(key = "departments") {
            DepartmentsCard(departments, onCreateDepartment, onDeleteDepartment)
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onCreate: (displayName: String) -> Job,
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

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.management_departments),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { creating = true }
            ) { Icon(Icons.Default.Add, null) }
        }
        if (departments == null) {
            Text("Loading...")
        } else if (departments.isEmpty()) {
            Text("No departments")
        } else {
            for (department in departments) {
                ListItem(
                    headlineContent = { Text(department.displayName) },
                    trailingContent = {
                        IconButton(
                            onClick = { deleting = department }
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun <T> DeleteDialog(
    item: T,
    displayName: (T) -> String,
    onDelete: () -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Delete ${displayName(item)}") },
        text = { Text("Are you sure you want to delete \"${displayName(item)}\"? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onDelete().invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}

@Composable
fun CreateDepartmentDialog(
    onCreate: (displayName: String) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create department") },
        text = {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                enabled = !isLoading
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName).invokeOnCompletion { onDismissRequested() }
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
