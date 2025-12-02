package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormFilePicker
import org.jetbrains.compose.resources.stringResource

typealias CreateInsuranceRequest = (company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate, document: PlatformFile?) -> Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInsuranceDialog(
    onCreate: CreateInsuranceRequest,
    onDismissRequest: () -> Unit,
) {
    var insuranceCompany by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf<LocalDate?>(null) }
    var validTo by remember { mutableStateOf<LocalDate?>(null) }
    var document by remember { mutableStateOf<PlatformFile?>(null) }

    val isValid = insuranceCompany.isNotBlank() && policyNumber.isNotBlank() && validFrom != null && validTo != null
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
                        listOf("FEMECV", "Rocalsub").forEach { company ->
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
                DatePickerFormField(
                    value = validFrom,
                    onValueChange = { validFrom = it },
                    label = stringResource(Res.string.insurance_start_date),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )
                DatePickerFormField(
                    value = validTo,
                    onValueChange = { validTo = it },
                    label = stringResource(Res.string.insurance_end_date),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )

                FormFilePicker(
                    label = stringResource(Res.string.insurance_document),
                    file = document,
                    onFilePicked = { document = it },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    pickerType = FileKitType.File("pdf"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    onCreate(
                        insuranceCompany,
                        policyNumber,
                        validFrom!!,
                        validTo!!,
                        document,
                    ).invokeOnCompletion {
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
