package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.Department
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,
    departments: List<Department>?
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
            DepartmentsCard(departments)
        }
    }
}

@Composable
fun DepartmentsCard(departments: List<Department>?) {
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
            for (department in departments) {
                ListItem(
                    headlineContent = { Text(department.displayName) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
