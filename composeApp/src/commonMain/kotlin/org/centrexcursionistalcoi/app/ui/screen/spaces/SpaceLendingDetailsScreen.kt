package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.form.FormFilesPicker
import org.centrexcursionistalcoi.app.utils.SpacePricing
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceLendingDetailsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * A space lending, for the user who made it and for the people who manage space lendings. A lending goes through
 * 3 steps: it is booked (and can be changed), a manager hands the keys over (which locks it), and the keys are
 * returned and it is paid.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpaceLendingDetailsScreen(
    lendingId: Uuid,
    onEdit: (SpaceLending) -> Unit,
    onBack: () -> Unit,
    model: SpaceLendingDetailsViewModel = koinViewModel { parametersOf(lendingId) },
) {
    val lending by model.lending.collectAsState()
    val space by model.space.collectAsState()
    val keys by model.keys.collectAsState()
    val profile by model.profile.collectAsState()
    val isWorking by model.isWorking.collectAsState()

    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAttendeesDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(space?.name ?: stringResource(Res.string.space_lending_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        val lending = lending
        val profile = profile
        if (lending == null || profile == null) {
            LoadingBox()
            return@Scaffold
        }
        val isOwner = lending.userSub != null && lending.userSub == profile.sub
        val isManager = profile.isSpaceLendingsManager
        val stage = lending.stage
        val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val isOver = today >= lending.checkOut

        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            item("stage") {
                AssistChip(onClick = {}, label = { Text(stage.label()) })
                Text(
                    "${lending.checkIn.formatted()} → ${lending.checkOut.formatted()} " +
                        "(${stringResource(Res.string.space_lending_nights, SpacePricing.nights(lending.checkIn, lending.checkOut))})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item("attendees") {
                Text(stringResource(Res.string.space_lending_attendees), style = MaterialTheme.typography.titleSmall)
                lending.attendees.filterValues { it > 0 }.forEach { (category, count) ->
                    Text("${category.label()}: $count")
                }
            }
            if (lending.keys.isNotEmpty()) {
                item("keys") {
                    Text(
                        stringResource(Res.string.space_lending_keys),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    lending.keys.forEach { key ->
                        val name = keys?.find { it.id == key.key }?.name.orEmpty()
                        Text(stringResource(Res.string.space_lending_keys_line, name, key.quantity))
                    }
                }
            }
            lending.notes?.let { notes ->
                item("notes") {
                    Text(
                        stringResource(Res.string.space_lending_notes),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(notes)
                }
            }
            item("price") {
                Text(
                    stringResource(Res.string.space_lending_price, formatPrice(lending.totalPrice)) +
                        " · " + lending.paymentStatus.label(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                lending.pickedUpAt?.let {
                    Text(stringResource(Res.string.space_lending_picked_up_by, it.toLocalDateTime(TimeZone.currentSystemDefault()).date.formatted()))
                }
                lending.returnedAt?.let {
                    Text(stringResource(Res.string.space_lending_returned_on, it.toLocalDateTime(TimeZone.currentSystemDefault()).date.formatted()))
                }
            }

            // Step 1: booked. It can be changed until the keys are handed over
            if (stage == SpaceLendingStage.CREATED && (isOwner || isManager)) {
                item("edit_actions") {
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedButton(onClick = { onEdit(lending) }, enabled = !isWorking) {
                            Text(stringResource(Res.string.space_lending_edit))
                        }
                        Spacer(Modifier.padding(4.dp))
                        OutlinedButton(onClick = { showCancelConfirmation = true }, enabled = !isWorking) {
                            Text(stringResource(Res.string.space_lending_cancel))
                        }
                    }
                }
            }
            if (stage != SpaceLendingStage.CANCELLED && stage != SpaceLendingStage.PAID && (isOwner || isManager)) {
                item("attendees_action") {
                    OutlinedButton(
                        onClick = { showAttendeesDialog = true },
                        enabled = !isWorking,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(Res.string.space_lending_attendees_edit))
                    }
                }
            }

            // Steps 2 and 3, for the people who manage lendings
            if (isManager) {
                item("manager_actions") {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        when (stage) {
                            SpaceLendingStage.CREATED -> {
                                Text(stringResource(Res.string.space_lending_pickup_hint), style = MaterialTheme.typography.bodySmall)
                                Button(onClick = model::pickup, enabled = !isWorking) {
                                    Text(stringResource(Res.string.space_lending_pickup))
                                }
                            }
                            SpaceLendingStage.PICKED_UP -> Button(onClick = model::returnKeys, enabled = !isWorking) {
                                Text(stringResource(Res.string.space_lending_return))
                            }
                            SpaceLendingStage.RETURNED -> Button(
                                onClick = { model.setPayment(PaymentStatus.COMPLETED) },
                                enabled = !isWorking,
                            ) {
                                Text(stringResource(Res.string.space_lending_payment_mark_paid))
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Payment: by bank transfer, attaching the confirmation
            if ((isOwner || isManager) && stage != SpaceLendingStage.CANCELLED && stage != SpaceLendingStage.PAID && lending.totalPrice > 0) {
                item("payment") {
                    PaymentProofSection(
                        proofs = lending.paymentProofs.size,
                        isWorking = isWorking,
                        onSend = { files, onDone -> model.attachPaymentProof(files, onDone) },
                    )
                }
            }

            // Notes and issues, once the lending is over
            if (isOwner && stage != SpaceLendingStage.CANCELLED) {
                item("report") {
                    if (lending.reportSubmittedAt != null) {
                        ReportSummary(lending)
                    } else if (isOver) {
                        ReportForm(
                            isWorking = isWorking,
                            onSend = { notes, issues, notesFiles, issuesFiles, onDone ->
                                model.submitReport(notes, issues, notesFiles, issuesFiles, onDone)
                            },
                        )
                    } else {
                        Text(
                            stringResource(Res.string.space_lending_report_available_later),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            } else if (isManager && lending.reportSubmittedAt != null) {
                item("report_summary") { ReportSummary(lending) }
            }
            item("bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }

        if (showCancelConfirmation) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmation = false },
                text = { Text(stringResource(Res.string.space_lending_cancel_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showCancelConfirmation = false
                        model.cancel()
                    }) { Text(stringResource(Res.string.space_lending_cancel)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmation = false }) {
                        Text(stringResource(Res.string.space_lending_dismiss))
                    }
                },
            )
        }
        if (showAttendeesDialog) {
            AttendeesDialog(
                initial = lending.attendees,
                onDismiss = { showAttendeesDialog = false },
                onSave = {
                    showAttendeesDialog = false
                    model.updateAttendees(it)
                },
            )
        }
    }
}

@Composable
private fun AttendeesDialog(
    initial: Map<Category, Int>,
    onDismiss: () -> Unit,
    onSave: (Map<Category, Int>) -> Unit,
) {
    var attendees by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.space_lending_attendees_edit)) },
        text = {
            Column {
                Text(stringResource(Res.string.space_lending_attendees_hint), style = MaterialTheme.typography.bodySmall)
                Category.entries.forEach { category ->
                    Stepper(
                        label = category.label(),
                        value = attendees[category] ?: 0,
                        max = null,
                        onChange = { attendees = attendees + (category to it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(attendees.filterValues { it > 0 }) },
                enabled = attendees.values.sum() > 0,
            ) { Text(stringResource(Res.string.space_lending_save_short)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.space_lending_dismiss)) }
        },
    )
}

@Composable
private fun PaymentProofSection(
    proofs: Int,
    isWorking: Boolean,
    onSend: (List<PlatformFile>, onDone: () -> Unit) -> Unit,
) {
    var files by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(Res.string.space_lending_payment), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.space_lending_payment_hint), style = MaterialTheme.typography.bodySmall)
        if (proofs > 0) Text(stringResource(Res.string.space_lending_payment_proofs, proofs))
        FormFilesPicker(
            files = files,
            onFilesChange = { files = it },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSend(files) { files = emptyList() } },
            enabled = !isWorking && files.isNotEmpty(),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.space_lending_payment_proof))
        }
    }
}

@Composable
private fun ReportForm(
    isWorking: Boolean,
    onSend: (String?, String?, List<PlatformFile>, List<PlatformFile>, onDone: () -> Unit) -> Unit,
) {
    var notes by rememberSaveable { mutableStateOf("") }
    var issues by rememberSaveable { mutableStateOf("") }
    var notesFiles by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
    var issuesFiles by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(Res.string.space_lending_report), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.space_lending_report_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.space_lending_report_notes)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        FormFilesPicker(
            files = notesFiles,
            onFilesChange = { notesFiles = it },
            label = stringResource(Res.string.space_lending_report_notes_photos),
            enabled = !isWorking,
            pickerType = FileKitType.Image,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = issues,
            onValueChange = { issues = it },
            label = { Text(stringResource(Res.string.space_lending_report_issues)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        FormFilesPicker(
            files = issuesFiles,
            onFilesChange = { issuesFiles = it },
            label = stringResource(Res.string.space_lending_report_issues_photos),
            enabled = !isWorking,
            pickerType = FileKitType.Image,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSend(notes, issues, notesFiles, issuesFiles) {} },
            enabled = !isWorking && (notes.isNotBlank() || issues.isNotBlank() || notesFiles.isNotEmpty() || issuesFiles.isNotEmpty()),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.space_lending_send))
        }
    }
}

@Composable
private fun ReportSummary(lending: SpaceLending) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(Res.string.space_lending_report), style = MaterialTheme.typography.titleSmall)
        lending.reportNotes?.let {
            Text(stringResource(Res.string.space_lending_report_notes), style = MaterialTheme.typography.labelMedium)
            Text(it)
        }
        if (lending.reportNotesFiles.isNotEmpty()) {
            Text(stringResource(Res.string.space_lending_report_files, lending.reportNotesFiles.size))
        }
        lending.reportIssues?.let {
            Text(stringResource(Res.string.space_lending_report_issues), style = MaterialTheme.typography.labelMedium)
            Text(it)
        }
        if (lending.reportIssuesFiles.isNotEmpty()) {
            Text(stringResource(Res.string.space_lending_report_files, lending.reportIssuesFiles.size))
        }
    }
}
