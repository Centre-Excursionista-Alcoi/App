package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

enum class DurationOption(val duration: Duration) {
    HOURLY(1.hours),
    DAILY(1.days)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceDialog(
    onCreate: (name: String, description: String?, price: Double?, priceDuration: Duration, capacity: Int?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priceDuration by remember { mutableStateOf(DurationOption.DAILY) }
    var capacity by remember { mutableStateOf("") }

    val isValid = name.isNotBlank() && (price.isBlank() || price.toDoubleOrNull() != null)

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_space_create)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.management_space_name)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.management_space_description).optional()) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(stringResource(Res.string.management_space_price).optional()) },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.weight(3f),
                        trailingIcon = { Icon(Icons.Default.Euro, null) }
                    )
                    DropdownField(
                        value = priceDuration,
                        onValueChange = { priceDuration = it },
                        options = DurationOption.entries,
                        label = stringResource(Res.string.management_space_price_duration),
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        enabled = !isLoading,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && isValid,
                onClick = {
                    isLoading = true
                    onCreate(
                        name,
                        description.takeUnless { it.isBlank() },
                        price.toDoubleOrNull(),
                        priceDuration.duration,
                        capacity.toIntOrNull(),
                    ).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text(stringResource(Res.string.create)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
