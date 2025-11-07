package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.displayName
import org.centrexcursionistalcoi.app.ui.reusable.ColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.DropdownSelector
import org.centrexcursionistalcoi.app.viewmodel.LendingSignUpViewModel
import org.jetbrains.compose.resources.stringResource

typealias LendingPageOnCreate = (phoneNumber: String, sports: List<Sports>) -> Job

@Composable
fun LendingSignUpScreen(
    model: LendingSignUpViewModel = viewModel { LendingSignUpViewModel() },
    onBackRequested: () -> Unit
) {
    LendingUserSignUpPage(
        onCreate = model::signUpForLending,
        onBackRequested
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingUserSignUpPage(
    onCreate: LendingPageOnCreate,
    onBackRequested: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackRequested) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                title = { Text(stringResource(Res.string.lending_signup_title)) },
            )
        }
    ) { paddingValues ->
        ColumnWidthWrapper(
            modifier = Modifier.fillMaxSize().padding(8.dp).padding(paddingValues).verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(Res.string.lending_signup_message),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(12.dp))

            var phoneNumber by remember { mutableStateOf("") }
            var sports by remember { mutableStateOf(emptyList<Sports>()) }
            var conditionsAccepted by remember { mutableStateOf(false) }

            var isLoading by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(Res.string.lending_signup_phone)) },
                placeholder = { Text("+34123456789") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            DropdownSelector(
                selection = sports,
                options = Sports.entries,
                onSelectionChange = { sports = it },
                label = stringResource(Res.string.lending_signup_sports),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                itemToString = { it.displayName },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(conditionsAccepted, onCheckedChange = { conditionsAccepted = it }, enabled = !isLoading)
                Text(stringResource(Res.string.lending_signup_accept), Modifier.weight(1f).padding(start = 8.dp))
            }

            val valid =
                conditionsAccepted && phoneNumber.isNotBlank() && sports.isNotEmpty()

            OutlinedButton(
                onClick = {
                    isLoading = true
                    onCreate(phoneNumber, sports).invokeOnCompletion {
                        isLoading = false
                        onBackRequested()
                    }
                },
                enabled = valid && !isLoading,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Submit")
            }
        }
    }
}
