package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.back
import cea_app.composeapp.generated.resources.remove
import cea_app.composeapp.generated.resources.space_lending_accept_conditions
import cea_app.composeapp.generated.resources.space_lending_attendees
import cea_app.composeapp.generated.resources.space_lending_creation_title
import cea_app.composeapp.generated.resources.space_lending_dates
import cea_app.composeapp.generated.resources.space_lending_dates_hint
import cea_app.composeapp.generated.resources.space_lending_edit_title
import cea_app.composeapp.generated.resources.space_lending_keys
import cea_app.composeapp.generated.resources.space_lending_missing_conditions
import cea_app.composeapp.generated.resources.space_lending_missing_dates
import cea_app.composeapp.generated.resources.space_lending_missing_people
import cea_app.composeapp.generated.resources.space_lending_nights
import cea_app.composeapp.generated.resources.space_lending_notes
import cea_app.composeapp.generated.resources.space_lending_price
import cea_app.composeapp.generated.resources.space_lending_save
import cea_app.composeapp.generated.resources.space_lending_submit
import cea_app.composeapp.generated.resources.spaces_conditions
import cea_app.composeapp.generated.resources.spaces_occupied
import cea_app.composeapp.generated.resources.spaces_occupied_range
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.ui.data.FutureSelectableDates
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Remove
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.utils.SpacePricing
import org.centrexcursionistalcoi.app.utils.fromEpochMillis
import org.centrexcursionistalcoi.app.utils.toEpochMillis
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceLendingCreationViewModel
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceLendingFormTarget
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Books a space, or, if [lendingId] is given, changes a lending that hasn't been picked up.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpaceLendingCreationScreen(
    spaceId: Uuid,
    lendingId: Uuid?,
    onDone: (Uuid) -> Unit,
    onBack: () -> Unit,
    model: SpaceLendingCreationViewModel = koinViewModel { parametersOf(SpaceLendingFormTarget(spaceId, lendingId)) },
) {
    val space by model.space.collectAsState()
    val keys by model.keys.collectAsState()
    val attendees by model.attendees.collectAsState()
    val keyQuantities by model.keyQuantities.collectAsState()
    val notes by model.notes.collectAsState()
    val acceptedConditions by model.acceptedConditions.collectAsState()
    val occupancy by model.occupancy.collectAsState()
    val isWorking by model.isWorking.collectAsState()
    val isLoaded by model.isLoaded.collectAsState()

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (model.isEditing) Res.string.space_lending_edit_title else Res.string.space_lending_creation_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        val space = space
        if (space == null || !isLoaded) {
            LoadingBox()
            return@Scaffold
        }
        // The date picker is the only owner of the chosen dates: everything else reads them from it
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = model.initialDates?.first?.toEpochMillis(),
            initialSelectedEndDateMillis = model.initialDates?.second?.toEpochMillis(),
            selectableDates = FutureSelectableDates(from = today, inclusive = true),
        )
        val checkIn = pickerState.selectedStartDateMillis?.let(LocalDate::fromEpochMillis)
        // Picking a single day is a stay with no nights
        val checkOut = pickerState.selectedEndDateMillis?.let(LocalDate::fromEpochMillis) ?: checkIn
        val price = if (checkIn != null && checkOut != null) {
            SpacePricing.compute(space.prices, attendees, checkIn, checkOut)
        } else {
            null
        }

        val hasConditions = space.conditionsOfUse != null
        val canSubmit = !isWorking &&
            checkIn != null &&
            attendees.values.sum() > 0 &&
            (!hasConditions || acceptedConditions)

        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            item("dates_title") {
                Text(stringResource(Res.string.space_lending_dates), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.space_lending_dates_hint), style = MaterialTheme.typography.bodySmall)
            }
            item("dates") {
                DateRangePicker(
                    state = pickerState,
                    title = {},
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item("nights") {
                if (checkIn != null && checkOut != null) {
                    val nights = SpacePricing.nights(checkIn, checkOut)
                    Text(
                        text = pluralStringResource(
                            Res.plurals.space_lending_nights,
                            nights,
                            nights
                        )
                    )
                }
            }
            if (!occupancy.isNullOrEmpty()) {
                item("occupied_title") {
                    Text(
                        stringResource(Res.string.spaces_occupied),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(occupancy.orEmpty(), key = { "occupied_${it.checkIn}_${it.checkOut}" }) { range ->
                    Text(stringResource(Res.string.spaces_occupied_range, range.checkIn.formatted(), range.checkOut.formatted()))
                }
            }

            item("attendees_title") {
                Text(
                    stringResource(Res.string.space_lending_attendees),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(Category.entries, key = { "attendees_$it" }) { category ->
                Stepper(
                    label = category.label(),
                    value = attendees[category] ?: 0,
                    max = null,
                    onChange = { model.setAttendees(category, it) },
                )
            }

            if (!keys.isNullOrEmpty()) {
                item("keys_title") {
                    Text(
                        stringResource(Res.string.space_lending_keys),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(keys.orEmpty(), key = { "key_${it.id}" }) { key ->
                    Stepper(
                        label = key.name,
                        value = keyQuantities[key.id] ?: 0,
                        max = key.maxQuantity,
                        onChange = { model.setKeyQuantity(key.id, it) },
                    )
                }
            }

            item("notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = model::setNotes,
                    label = { Text(stringResource(Res.string.space_lending_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }

            space.conditionsOfUse?.let { conditions ->
                item("conditions") {
                    Text(
                        stringResource(Res.string.spaces_conditions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(conditions)
                    if (!model.isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Checkbox(checked = acceptedConditions, onCheckedChange = model::setAcceptedConditions)
                            Text(stringResource(Res.string.space_lending_accept_conditions))
                        }
                    }
                }
            }

            item("submit") {
                price?.let {
                    Text(
                        stringResource(Res.string.space_lending_price, formatPrice(it)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                // Say what is missing, instead of leaving the button disabled without a reason
                val missing = when {
                    checkIn == null -> Res.string.space_lending_missing_dates
                    attendees.values.sum() <= 0 -> Res.string.space_lending_missing_people
                    hasConditions && !acceptedConditions -> Res.string.space_lending_missing_conditions
                    else -> null
                }
                if (missing != null) {
                    Text(
                        stringResource(missing),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        if (checkIn != null && checkOut != null) model.submit(checkIn, checkOut, onDone)
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Text(stringResource(if (model.isEditing) Res.string.space_lending_save else Res.string.space_lending_submit))
                }
            }
        }
    }
}

/**
 * A row with a label, and buttons to decrease and increase a number between 0 and [max].
 */
@Composable
internal fun Stepper(
    label: String,
    value: Int,
    max: Int?,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > 0) {
            Icon(MaterialSymbols.Remove, stringResource(Res.string.remove))
        }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = { onChange(value + 1) }, enabled = enabled && (max == null || value < max)) {
            Icon(MaterialSymbols.Add, null)
        }
    }
}
