package org.centrexcursionistalcoi.app.ui.screen.spaces

import cea_app.composeapp.generated.resources.space_lending_refund_not_online
import cea_app.composeapp.generated.resources.space_lending_refund_rejected
import cea_app.composeapp.generated.resources.space_lending_refund_confirm
import cea_app.composeapp.generated.resources.space_lending_refund_online
import cea_app.composeapp.generated.resources.space_lending_pay_unavailable
import cea_app.composeapp.generated.resources.space_lending_pay_check
import cea_app.composeapp.generated.resources.space_lending_pay_waiting
import cea_app.composeapp.generated.resources.space_lending_pay_bizum
import cea_app.composeapp.generated.resources.space_lending_pay_card
import cea_app.composeapp.generated.resources.space_lending_pay_online_hint
import cea_app.composeapp.generated.resources.space_lending_pay_online
import org.centrexcursionistalcoi.app.data.PaymentMethod
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.back
import cea_app.composeapp.generated.resources.space_lending_audit_show
import cea_app.composeapp.generated.resources.space_lending_audit_hide
import cea_app.composeapp.generated.resources.space_lending_audit_empty
import cea_app.composeapp.generated.resources.space_lending_audit_by
import cea_app.composeapp.generated.resources.payment_audit_event_created
import cea_app.composeapp.generated.resources.payment_audit_event_create_failed
import cea_app.composeapp.generated.resources.payment_audit_event_result_received
import cea_app.composeapp.generated.resources.payment_audit_event_status_changed
import cea_app.composeapp.generated.resources.payment_audit_event_result_ignored
import cea_app.composeapp.generated.resources.payment_audit_event_unknown_reference
import cea_app.composeapp.generated.resources.payment_audit_event_notification_rejected
import cea_app.composeapp.generated.resources.payment_audit_event_refund_requested
import cea_app.composeapp.generated.resources.payment_audit_event_cancel_requested
import cea_app.composeapp.generated.resources.payment_audit_event_operation_failed
import cea_app.composeapp.generated.resources.payment_audit_event_listener_failed
import org.centrexcursionistalcoi.app.data.PaymentAuditEntry
import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import cea_app.composeapp.generated.resources.space_lending_attendees
import cea_app.composeapp.generated.resources.space_lending_attendees_edit
import cea_app.composeapp.generated.resources.space_lending_attendees_hint
import cea_app.composeapp.generated.resources.space_lending_cancel
import cea_app.composeapp.generated.resources.space_lending_cancel_confirm
import cea_app.composeapp.generated.resources.space_lending_dismiss
import cea_app.composeapp.generated.resources.space_lending_edit
import cea_app.composeapp.generated.resources.space_lending_keys
import cea_app.composeapp.generated.resources.space_lending_keys_line
import cea_app.composeapp.generated.resources.space_lending_nights
import cea_app.composeapp.generated.resources.space_lending_notes
import cea_app.composeapp.generated.resources.space_lending_payment
import cea_app.composeapp.generated.resources.space_lending_payment_hint
import cea_app.composeapp.generated.resources.space_lending_payment_mark_paid
import cea_app.composeapp.generated.resources.space_lending_payment_mark_paid_hint
import cea_app.composeapp.generated.resources.space_lending_payment_mark_paid_title
import cea_app.composeapp.generated.resources.space_lending_payment_undo
import cea_app.composeapp.generated.resources.space_lending_payment_undo_confirm
import cea_app.composeapp.generated.resources.space_lending_paid_by
import cea_app.composeapp.generated.resources.manual_payment_method_bizum
import cea_app.composeapp.generated.resources.manual_payment_method_card
import cea_app.composeapp.generated.resources.manual_payment_method_cash
import cea_app.composeapp.generated.resources.manual_payment_method_other
import cea_app.composeapp.generated.resources.manual_payment_method_transfer
import cea_app.composeapp.generated.resources.payment_audit_event_manual_payment_set
import cea_app.composeapp.generated.resources.payment_audit_event_manual_payment_undone
import org.centrexcursionistalcoi.app.data.ManualPaymentMethod
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import cea_app.composeapp.generated.resources.space_lending_payment_proof
import cea_app.composeapp.generated.resources.space_lending_payment_proofs
import cea_app.composeapp.generated.resources.space_lending_picked_up_by
import cea_app.composeapp.generated.resources.space_lending_pickup
import cea_app.composeapp.generated.resources.space_lending_pickup_hint
import cea_app.composeapp.generated.resources.space_lending_price
import cea_app.composeapp.generated.resources.space_lending_report
import cea_app.composeapp.generated.resources.space_lending_report_available_later
import cea_app.composeapp.generated.resources.space_lending_report_files
import cea_app.composeapp.generated.resources.space_lending_report_hint
import cea_app.composeapp.generated.resources.space_lending_report_issues
import cea_app.composeapp.generated.resources.space_lending_report_issues_photos
import cea_app.composeapp.generated.resources.space_lending_report_notes
import cea_app.composeapp.generated.resources.space_lending_report_notes_photos
import cea_app.composeapp.generated.resources.space_lending_return
import cea_app.composeapp.generated.resources.space_lending_returned_on
import cea_app.composeapp.generated.resources.space_lending_save_short
import cea_app.composeapp.generated.resources.space_lending_send
import cea_app.composeapp.generated.resources.space_lending_title
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
import org.jetbrains.compose.resources.pluralStringResource
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
    val keyTypes by model.keyTypes.collectAsState()
    val profile by model.profile.collectAsState()
    val isWorking by model.isWorking.collectAsState()
    val paymentIssue by model.paymentIssue.collectAsState()
    val awaitingPayment by model.awaitingPayment.collectAsState()
    val auditLog by model.auditLog.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showRefundConfirmation by rememberSaveable { mutableStateOf(false) }
    var showMarkPaidDialog by rememberSaveable { mutableStateOf(false) }
    var showUndoPaymentConfirmation by rememberSaveable { mutableStateOf(false) }

    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAttendeesDialog by rememberSaveable { mutableStateOf(false) }
    var showPickupDialog by rememberSaveable { mutableStateOf(false) }
    var showReturnDialog by rememberSaveable { mutableStateOf(false) }
    val clubKeys by model.keys.collectAsState()
    val keysOut by model.keysOut.collectAsState()
    val keysOutOrEmpty = keysOut.orEmpty()

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
                val nights = SpacePricing.nights(lending.checkIn, lending.checkOut)

                AssistChip(onClick = {}, label = { Text(stage.label()) })
                Text(
                    "${lending.checkIn.formatted()} → ${lending.checkOut.formatted()} " +
                        "(${pluralStringResource(Res.plurals.space_lending_nights, nights, nights)})",
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
            if (lending.requestedKeys.isNotEmpty()) {
                item("keys") {
                    Text(
                        stringResource(Res.string.space_lending_keys),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    lending.requestedKeys.forEach { (typeId, quantity) ->
                        val name = keyTypes?.find { it.id == typeId }?.name.orEmpty()
                        Text(stringResource(Res.string.space_lending_keys_line, name, quantity))
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
                                Button(
                                    onClick = {
                                        if (lending.requestedKeys.isEmpty()) model.pickup(emptyList()) else showPickupDialog = true
                                    },
                                    enabled = !isWorking,
                                ) {
                                    Text(stringResource(Res.string.space_lending_pickup))
                                }
                            }
                            SpaceLendingStage.PICKED_UP -> Button(
                                onClick = {
                                    if (lending.keys.none { it.isOut }) model.returnKeys(null) else showReturnDialog = true
                                },
                                enabled = !isWorking,
                            ) {
                                Text(stringResource(Res.string.space_lending_return))
                            }
                            SpaceLendingStage.RETURNED -> Button(
                                onClick = { showMarkPaidDialog = true },
                                enabled = !isWorking,
                            ) {
                                Text(stringResource(Res.string.space_lending_payment_mark_paid))
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Payment online, by the owner, once the keys are back
            if (isOwner && stage == SpaceLendingStage.RETURNED && lending.totalPrice > 0 && model.paymentMethods.isNotEmpty()) {
                item("payment_online") {
                    OnlinePaymentSection(
                        price = formatPrice(lending.totalPrice),
                        bizum = PaymentMethod.BIZUM in model.paymentMethods,
                        isWorking = isWorking,
                        awaiting = awaitingPayment,
                        onPay = { method -> model.pay(method, uriHandler::openUri) },
                        onCheck = model::checkPayment,
                    )
                }
            }
            if (paymentIssue != null) {
                item("payment_issue") {
                    Text(
                        stringResource(
                            when (paymentIssue) {
                                SpaceLendingDetailsViewModel.PaymentIssue.GATEWAY_UNAVAILABLE -> Res.string.space_lending_pay_unavailable
                                SpaceLendingDetailsViewModel.PaymentIssue.REFUND_REJECTED -> Res.string.space_lending_refund_rejected
                                else -> Res.string.space_lending_refund_not_online
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            // A payment a manager recorded by hand (cash...) can be undone, but not returned: there's no provider to ask
            val manualMethod = lending.manualPaymentMethod
            if (manualMethod != null && lending.paymentStatus == PaymentStatus.COMPLETED) {
                item("manual_payment") {
                    Text(
                        stringResource(Res.string.space_lending_paid_by, manualMethod.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (isManager) {
                    item("undo_payment") {
                        OutlinedButton(
                            onClick = { showUndoPaymentConfirmation = true },
                            enabled = !isWorking,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(Res.string.space_lending_payment_undo))
                        }
                    }
                }
            }
            // A payment through the gateway is never undone: a manager returns the money instead
            if (isManager && manualMethod == null && lending.totalPrice > 0 && lending.paymentStatus == PaymentStatus.COMPLETED) {
                item("refund_online") {
                    OutlinedButton(
                        onClick = { showRefundConfirmation = true },
                        enabled = !isWorking,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(Res.string.space_lending_refund_online))
                    }
                }
            }

            // Admins can trace everything that happened with the payments
            if (profile.isAdmin) {
                item("payment_audit") {
                    PaymentAuditSection(
                        entries = auditLog,
                        isWorking = isWorking,
                        onLoad = { model.loadAuditLog() },
                    )
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

        if (showPickupDialog) {
            SpaceKeysPickupDialog(
                requested = lending.requestedKeys,
                keyTypes = keyTypes.orEmpty(),
                keys = clubKeys.orEmpty().filter { it.id !in keysOutOrEmpty },
                onDismiss = { showPickupDialog = false },
                onConfirm = {
                    showPickupDialog = false
                    model.pickup(it)
                },
            )
        }
        if (showReturnDialog) {
            SpaceKeysReturnDialog(
                given = lending.keys.filter { it.isOut },
                keys = clubKeys.orEmpty(),
                keyTypes = keyTypes.orEmpty(),
                onDismiss = { showReturnDialog = false },
                onConfirm = {
                    showReturnDialog = false
                    model.returnKeys(it)
                },
            )
        }
        if (showMarkPaidDialog) {
            MarkPaidDialog(
                onDismiss = { showMarkPaidDialog = false },
                onConfirm = {
                    showMarkPaidDialog = false
                    model.markPaid(it)
                },
            )
        }
        if (showUndoPaymentConfirmation) {
            AlertDialog(
                onDismissRequest = { showUndoPaymentConfirmation = false },
                text = { Text(stringResource(Res.string.space_lending_payment_undo_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showUndoPaymentConfirmation = false
                        model.undoPayment()
                    }) { Text(stringResource(Res.string.space_lending_payment_undo)) }
                },
                dismissButton = {
                    TextButton(onClick = { showUndoPaymentConfirmation = false }) {
                        Text(stringResource(Res.string.space_lending_dismiss))
                    }
                },
            )
        }
        if (showRefundConfirmation) {
            AlertDialog(
                onDismissRequest = { showRefundConfirmation = false },
                text = { Text(stringResource(Res.string.space_lending_refund_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRefundConfirmation = false
                        model.refundOnline()
                    }) { Text(stringResource(Res.string.space_lending_refund_online)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRefundConfirmation = false }) {
                        Text(stringResource(Res.string.space_lending_dismiss))
                    }
                },
            )
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
private fun MarkPaidDialog(
    onDismiss: () -> Unit,
    onConfirm: (ManualPaymentMethod) -> Unit,
) {
    var method by rememberSaveable { mutableStateOf(ManualPaymentMethod.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.space_lending_payment_mark_paid_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.space_lending_payment_mark_paid_hint), style = MaterialTheme.typography.bodySmall)
                ManualPaymentMethod.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = method == option, role = Role.RadioButton, onClick = { method = option }),
                    ) {
                        RadioButton(selected = method == option, onClick = null)
                        Text(option.label(), modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(method) }) { Text(stringResource(Res.string.space_lending_payment_mark_paid)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.space_lending_dismiss)) }
        },
    )
}

@Composable
private fun ManualPaymentMethod.label(): String = stringResource(
    when (this) {
        ManualPaymentMethod.CASH -> Res.string.manual_payment_method_cash
        ManualPaymentMethod.CARD -> Res.string.manual_payment_method_card
        ManualPaymentMethod.BIZUM -> Res.string.manual_payment_method_bizum
        ManualPaymentMethod.TRANSFER -> Res.string.manual_payment_method_transfer
        ManualPaymentMethod.OTHER -> Res.string.manual_payment_method_other
    }
)

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
private fun OnlinePaymentSection(
    price: String,
    bizum: Boolean,
    isWorking: Boolean,
    awaiting: Boolean,
    onPay: (PaymentMethod) -> Unit,
    onCheck: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(Res.string.space_lending_pay_online), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.space_lending_pay_online_hint, price), style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onPay(PaymentMethod.CARD) }, enabled = !isWorking) {
                Text(stringResource(Res.string.space_lending_pay_card))
            }
            if (bizum) {
                Button(onClick = { onPay(PaymentMethod.BIZUM) }, enabled = !isWorking) {
                    Text(stringResource(Res.string.space_lending_pay_bizum))
                }
            }
        }
        if (awaiting) {
            Text(
                stringResource(Res.string.space_lending_pay_waiting),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(onClick = onCheck, enabled = !isWorking) {
                Text(stringResource(Res.string.space_lending_pay_check))
            }
        }
    }
}

@Composable
private fun PaymentAuditSection(
    entries: List<PaymentAuditEntry>?,
    isWorking: Boolean,
    onLoad: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        OutlinedButton(
            onClick = {
                expanded = !expanded
                if (expanded) onLoad()
            },
            enabled = !isWorking,
        ) {
            Text(stringResource(if (expanded) Res.string.space_lending_audit_hide else Res.string.space_lending_audit_show))
        }
        if (expanded && entries != null) {
            if (entries.isEmpty()) {
                Text(
                    stringResource(Res.string.space_lending_audit_empty),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            for (entry in entries) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(entry.event.label(), style = MaterialTheme.typography.titleSmall)
                    val change = listOfNotNull(entry.previousStatus, entry.newStatus).distinct().joinToString(" → ")
                    val amount = entry.amountCents?.let { "${it / 100}.${(it % 100).toString().padStart(2, '0')} €" }
                    val details = listOfNotNull(change.ifEmpty { null }, amount).joinToString(" · ")
                    if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall)
                    val time = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString().replace('T', ' ')
                    Text(
                        "$time · " + stringResource(Res.string.space_lending_audit_by, entry.actor),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentAuditEvent.label(): String = stringResource(
    when (this) {
        PaymentAuditEvent.CREATED -> Res.string.payment_audit_event_created
        PaymentAuditEvent.CREATE_FAILED -> Res.string.payment_audit_event_create_failed
        PaymentAuditEvent.RESULT_RECEIVED -> Res.string.payment_audit_event_result_received
        PaymentAuditEvent.STATUS_CHANGED -> Res.string.payment_audit_event_status_changed
        PaymentAuditEvent.RESULT_IGNORED -> Res.string.payment_audit_event_result_ignored
        PaymentAuditEvent.UNKNOWN_REFERENCE -> Res.string.payment_audit_event_unknown_reference
        PaymentAuditEvent.NOTIFICATION_REJECTED -> Res.string.payment_audit_event_notification_rejected
        PaymentAuditEvent.REFUND_REQUESTED -> Res.string.payment_audit_event_refund_requested
        PaymentAuditEvent.CANCEL_REQUESTED -> Res.string.payment_audit_event_cancel_requested
        PaymentAuditEvent.OPERATION_FAILED -> Res.string.payment_audit_event_operation_failed
        PaymentAuditEvent.LISTENER_FAILED -> Res.string.payment_audit_event_listener_failed
        PaymentAuditEvent.MANUAL_PAYMENT_SET -> Res.string.payment_audit_event_manual_payment_set
        PaymentAuditEvent.MANUAL_PAYMENT_UNDONE -> Res.string.payment_audit_event_manual_payment_undone
    }
)

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
            Text(pluralStringResource(Res.plurals.space_lending_report_files, lending.reportNotesFiles.size, lending.reportNotesFiles.size))
        }
        lending.reportIssues?.let {
            Text(stringResource(Res.string.space_lending_report_issues), style = MaterialTheme.typography.labelMedium)
            Text(it)
        }
        if (lending.reportIssuesFiles.isNotEmpty()) {
            Text(pluralStringResource(Res.plurals.space_lending_report_files, lending.reportIssuesFiles.size, lending.reportIssuesFiles.size))
        }
    }
}
