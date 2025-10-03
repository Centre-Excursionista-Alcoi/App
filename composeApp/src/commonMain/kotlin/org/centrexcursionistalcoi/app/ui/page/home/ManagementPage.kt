package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?,
    onDeleteDepartment: (Department) -> Job
) {
    val columns = if (windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium) {
        // Desktop and large tablets
        GridCells.Adaptive(minSize = 300.dp)
    } else {
        // Phones and small tablets
        GridCells.Fixed(1)
    }
    LazyVerticalGrid(
        columns = columns,
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item(key = "departments") {
            DepartmentsCard(departments, onDeleteDepartment)
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onDelete: (Department) -> Job
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.management_departments),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
        if (departments == null) {
            Text("Loading...")
        } else if (departments.isEmpty()) {
            Text("No departments")
        } else {
            var deleting by remember { mutableStateOf<Department?>(null) }
            deleting?.let { department ->
                var isLoading by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { if (!isLoading) deleting = null },
                    title = { Text("Delete department") },
                    text = { Text("Are you sure you want to delete the department \"${department.displayName}\"? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            enabled = !isLoading,
                            onClick = {
                                isLoading = true
                                onDelete(department).invokeOnCompletion { deleting = null }
                            }
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !isLoading,
                            onClick = { deleting = null }
                        ) { Text("Cancel") }
                    },
                )
            }

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
