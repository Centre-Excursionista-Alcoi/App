package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

typealias CreateInsuranceRequest = (company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate) -> Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInsuranceDialog(
    onCreate: CreateInsuranceRequest,
    onDismissRequest: () -> Unit,
) {
    var insuranceCompany by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }

    val isFromValid = try {
        LocalDate.parse(validFrom)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
    val isToValid = try {
        LocalDate.parse(validTo)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
    val isValid = insuranceCompany.isNotBlank() && policyNumber.isNotBlank() && isFromValid && isToValid
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.insurance_add_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                var showingCompanySuggestions by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showingCompanySuggestions && !isLoading,
                    onExpandedChange = { showingCompanySuggestions = it }
                ) {
                    OutlinedTextField(
                        value = insuranceCompany,
                        onValueChange = { insuranceCompany = it },
                        label = { Text(stringResource(Res.string.insurance_company)) },
                        maxLines = 1,
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showingCompanySuggestions && !isLoading,
                        onDismissRequest = { showingCompanySuggestions = false }
                    ) {
                        listOf("FEMECV").forEach { company ->
                            if (company.contains(insuranceCompany, ignoreCase = true)) {
                                DropdownMenuItem(
                                    enabled = !isLoading,
                                    text = { Text(company) },
                                    onClick = {
                                        insuranceCompany = company
                                        showingCompanySuggestions = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = policyNumber,
                    onValueChange = { policyNumber = it },
                    label = { Text(stringResource(Res.string.insurance_policy_number)) },
                    maxLines = 1,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = validFrom,
                    onValueChange = { validFrom = it },
                    label = { Text(stringResource(Res.string.insurance_start_date)) },
                    placeholder = { Text("yyyy-MM-dd") },
                    maxLines = 1,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = validTo,
                    onValueChange = { validTo = it },
                    label = { Text(stringResource(Res.string.insurance_end_date)) },
                    placeholder = { Text("yyyy-MM-dd") },
                    maxLines = 1,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    onCreate(insuranceCompany, policyNumber, LocalDate.parse(validFrom), LocalDate.parse(validTo)).invokeOnCompletion {
                        isLoading = false
                        if (it == null) onDismissRequest()
                    }
                },
                enabled = !isLoading && isValid
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
