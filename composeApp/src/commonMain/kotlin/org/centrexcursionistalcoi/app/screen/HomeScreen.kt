package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.auth.UserData
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(userData: UserData, model: HomeViewModel = viewModel { HomeViewModel() }) {
    LaunchedEffect(Unit) { model.loadDepartments() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Handle navigation item click */ },
                    label = { Text("Home") },
                    icon = { /* Icon can be added here */ }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = "\uD83D\uDC4B\uD83C\uDFFB Welcome back ${userData.name}!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
            )

            var departmentName by remember { mutableStateOf("") }
            OutlinedTextField(
                value = departmentName,
                onValueChange = { departmentName = it },
                label = { Text("Create Department") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                trailingIcon = {
                    Text(
                        text = "\uD83D\uDD12",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                model.createDepartment(departmentName)
                                departmentName = ""
                            }
                    )
                }
            )
        }
    }
}
