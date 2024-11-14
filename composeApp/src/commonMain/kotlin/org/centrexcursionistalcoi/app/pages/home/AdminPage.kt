package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.Res
import ceaapp.composeapp.generated.resources.add
import ceaapp.composeapp.generated.resources.bookings_confirm
import ceaapp.composeapp.generated.resources.bookings_empty
import ceaapp.composeapp.generated.resources.bookings_from
import ceaapp.composeapp.generated.resources.bookings_hide_complete
import ceaapp.composeapp.generated.resources.bookings_info
import ceaapp.composeapp.generated.resources.bookings_items
import ceaapp.composeapp.generated.resources.bookings_list
import ceaapp.composeapp.generated.resources.bookings_made_by
import ceaapp.composeapp.generated.resources.bookings_mark_returned
import ceaapp.composeapp.generated.resources.bookings_mark_taken
import ceaapp.composeapp.generated.resources.bookings_not_returned
import ceaapp.composeapp.generated.resources.bookings_not_taken
import ceaapp.composeapp.generated.resources.bookings_pending
import ceaapp.composeapp.generated.resources.bookings_returned
import ceaapp.composeapp.generated.resources.bookings_returned_at
import ceaapp.composeapp.generated.resources.bookings_taken_at
import ceaapp.composeapp.generated.resources.bookings_to
import ceaapp.composeapp.generated.resources.confirm
import ceaapp.composeapp.generated.resources.create
import ceaapp.composeapp.generated.resources.delete
import ceaapp.composeapp.generated.resources.items_details_future_booking
import ceaapp.composeapp.generated.resources.items_details_future_booking_not_confirmed
import ceaapp.composeapp.generated.resources.items_details_future_bookings
import ceaapp.composeapp.generated.resources.items_details_not_booked
import ceaapp.composeapp.generated.resources.items_details_taken
import ceaapp.composeapp.generated.resources.items_details_title
import ceaapp.composeapp.generated.resources.items_health
import ceaapp.composeapp.generated.resources.items_health_value
import ceaapp.composeapp.generated.resources.items_notes
import ceaapp.composeapp.generated.resources.items_notes_value
import ceaapp.composeapp.generated.resources.items_title
import ceaapp.composeapp.generated.resources.items_type
import ceaapp.composeapp.generated.resources.items_type_value
import ceaapp.composeapp.generated.resources.sections_create
import ceaapp.composeapp.generated.resources.sections_empty
import ceaapp.composeapp.generated.resources.sections_name
import ceaapp.composeapp.generated.resources.sections_title
import ceaapp.composeapp.generated.resources.select
import ceaapp.composeapp.generated.resources.selected_size
import ceaapp.composeapp.generated.resources.types_brand
import ceaapp.composeapp.generated.resources.types_create
import ceaapp.composeapp.generated.resources.types_description
import ceaapp.composeapp.generated.resources.types_empty
import ceaapp.composeapp.generated.resources.types_image
import ceaapp.composeapp.generated.resources.types_model
import ceaapp.composeapp.generated.resources.types_name
import ceaapp.composeapp.generated.resources.types_section
import ceaapp.composeapp.generated.resources.types_title
import ceaapp.composeapp.generated.resources.unconfirmed_users_email
import ceaapp.composeapp.generated.resources.unconfirmed_users_full_name
import ceaapp.composeapp.generated.resources.unconfirmed_users_message
import ceaapp.composeapp.generated.resources.unconfirmed_users_phone
import ceaapp.composeapp.generated.resources.unconfirmed_users_title
import ceaapp.composeapp.generated.resources.update
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.composition.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.data.fromDate
import org.centrexcursionistalcoi.app.data.health
import org.centrexcursionistalcoi.app.data.localizedName
import org.centrexcursionistalcoi.app.data.returnedDate
import org.centrexcursionistalcoi.app.data.takenDate
import org.centrexcursionistalcoi.app.data.toDate
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformCheckbox
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.DatabaseData
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.LendingD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.centrexcursionistalcoi.app.server.response.data.UserD
import org.centrexcursionistalcoi.app.server.response.data.enumeration.ItemHealth
import org.centrexcursionistalcoi.app.utils.humanReadableSize
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdminPage(
    updatingUser: Boolean,
    users: List<UserD>?,
    onUserConfirmationRequested: (UserD, () -> Unit) -> Unit,
    onUserDeleteRequested: (UserD, () -> Unit) -> Unit,
    isCreatingSection: Boolean,
    sections: List<SectionD>?,
    onSectionOperation: (SectionD, onCreate: () -> Unit) -> Unit,
    itemTypes: List<ItemTypeD>?,
    isCreatingType: Boolean,
    onTypeOperation: (ItemTypeD, onCreate: () -> Unit) -> Unit,
    items: List<ItemD>?,
    isCreatingItem: Boolean,
    onItemOperation: (ItemD, onCreate: () -> Unit) -> Unit,
    allBookings: List<LendingD>?,
    isUpdatingBooking: Boolean,
    onConfirmBookingRequested: (LendingD, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (LendingD, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (LendingD, () -> Unit) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        users
            ?.filter { !it.isConfirmed }
            ?.takeIf { it.isNotEmpty() }
            ?.let { unconfirmedUsers ->
                UnconfirmedUsersCard(unconfirmedUsers, updatingUser, onUserConfirmationRequested, onUserDeleteRequested)
            }

        SectionsCard(sections, isCreatingSection, onSectionOperation)

        TypesCard(itemTypes, sections, isCreatingType, onTypeOperation)

        ItemsCard(items, itemTypes, isCreatingItem, onItemOperation, allBookings)

        BookingsCard(
            allBookings,
            items,
            itemTypes,
            isUpdatingBooking,
            onConfirmBookingRequested,
            onMarkAsTakenRequested,
            onMarkAsReturnedRequested
        )
    }
}

@Composable
private fun <Type : DatabaseData> CreationDialog(
    showingCreationDialog: Type?,
    title: StringResource,
    isCreating: Boolean,
    onCreateRequested: (Type, onCreate: () -> Unit) -> Unit,
    onDismissRequested: () -> Unit,
    content: @Composable ColumnScope.(Type) -> Unit
) {
    showingCreationDialog?.let { data ->
        PlatformDialog(
            onDismissRequest = { if (!isCreating) onDismissRequested() }
        ) {
            BasicText(
                text = stringResource(title),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            content(data)
            PlatformButton(
                text = stringResource(if (data.id == null) Res.string.create else Res.string.update),
                modifier = Modifier.align(Alignment.End).padding(8.dp),
                enabled = !isCreating
            ) {
                onCreateRequested(data, onDismissRequested)
            }
        }
    }
}

@Composable
fun UnconfirmedUsersCard(
    unconfirmedUsers: List<UserD>,
    isConfirming: Boolean,
    onConfirmRequested: (UserD, onComplete: () -> Unit) -> Unit,
    onDeleteRequested: (UserD, onComplete: () -> Unit) -> Unit
) {
    var confirmingUser: UserD? by remember { mutableStateOf(null) }
    confirmingUser?.let { user ->
        PlatformDialog(
            onDismissRequest = { confirmingUser = null }
        ) {
            BasicText(
                text = stringResource(Res.string.unconfirmed_users_message),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            BasicText(
                text = stringResource(Res.string.unconfirmed_users_full_name, user.name + " " + user.familyName),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            BasicText(
                text = stringResource(Res.string.unconfirmed_users_email, user.email),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            BasicText(
                text = stringResource(Res.string.unconfirmed_users_email, user.email),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            BasicText(
                text = stringResource(Res.string.unconfirmed_users_phone, user.phone),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PlatformButton(
                    text = stringResource(Res.string.delete),
                    modifier = Modifier.padding(end = 8.dp).padding(vertical = 8.dp),
                    enabled = !isConfirming
                ) {
                    onDeleteRequested(user) { confirmingUser = null }
                }
                PlatformButton(
                    text = stringResource(Res.string.confirm),
                    modifier = Modifier.padding(end = 8.dp).padding(vertical = 8.dp),
                    enabled = !isConfirming
                ) {
                    onConfirmRequested(user) { confirmingUser = null }
                }
            }
        }
    }

    PlatformCard(
        title = stringResource(Res.string.unconfirmed_users_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        for (user in unconfirmedUsers) {
            PlatformCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { confirmingUser = user }
            ) {
                BasicText(
                    text = user.email,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 8.dp),
                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SectionsCard(
    sections: List<SectionD>?,
    isCreating: Boolean,
    onOperationRequested: (SectionD, onCreate: () -> Unit) -> Unit,
) {
    var showingCreationDialog: SectionD? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.sections_create,
        isCreating = isCreating,
        onCreateRequested = onOperationRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        PlatformFormField(
            value = data.displayName,
            onValueChange = { showingCreationDialog = data.copy(displayName = it) },
            label = stringResource(Res.string.sections_name),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
    }

    PlatformCard(
        title = stringResource(Res.string.sections_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add), { showingCreationDialog = SectionD() }),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = sections,
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (list == null) {
                    PlatformLoadingIndicator(large = false)
                } else {
                    if (list.isEmpty()) {
                        BasicText(
                            text = stringResource(Res.string.sections_empty),
                            style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                    for (item in list) {
                        PlatformCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { showingCreationDialog = item }
                        ) {
                            BasicText(
                                text = item.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun TypesCard(
    itemTypes: List<ItemTypeD>?,
    sections: List<SectionD>?,
    isCreating: Boolean,
    onCreateRequested: (ItemTypeD, onCreate: () -> Unit) -> Unit
) {
    var showingCreationDialog: ItemTypeD? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.types_create,
        isCreating = isCreating,
        onCreateRequested = onCreateRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        PlatformFormField(
            value = data.title,
            onValueChange = { showingCreationDialog = data.copy(title = it) },
            label = stringResource(Res.string.types_name),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformTextArea(
            value = data.description ?: "",
            onValueChange = { showingCreationDialog = data.copy(description = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_description),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.brand ?: "",
            onValueChange = { showingCreationDialog = data.copy(brand = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_brand),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformFormField(
            value = data.model ?: "",
            onValueChange = { showingCreationDialog = data.copy(model = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.types_model),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )

        val filePicker = rememberFilePickerLauncher(mode = PickerMode.Single, type = PickerType.Image) { file ->
            CoroutineScope(Dispatchers.Main).launch {
                val bytes = file?.readBytes()
                val base64 = bytes?.let { Base64.encode(bytes) }
                showingCreationDialog = data.copy(imageBytesBase64 = base64)
            }
        }
        BasicText(
            text = stringResource(Res.string.types_image),
            style = getPlatformTextStyles().label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        PlatformButton(
            text = if (data.imageBytesBase64 == null)
                stringResource(Res.string.select)
            else
                stringResource(Res.string.selected_size, data.imageBytes()!!.humanReadableSize()),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        ) { filePicker.launch() }

        PlatformDropdown(
            value = data.sectionId?.let { sectionId -> sections?.find { it.id == sectionId } },
            onValueChange = { showingCreationDialog = data.copy(sectionId = it.id) },
            options = sections ?: emptyList(),
            label = stringResource(Res.string.types_section),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            toString = { it?.displayName ?: "" }
        )
    }

    PlatformCard(
        title = stringResource(Res.string.types_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add)) { showingCreationDialog = ItemTypeD() },
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = itemTypes,
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (list == null) {
                    PlatformLoadingIndicator(large = false)
                } else if (list.isEmpty()) {
                    BasicText(
                        text = stringResource(Res.string.types_empty),
                        style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    val windowSizeClass = calculateWindowSizeClass()
                    val groupCount = when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 1
                        WindowWidthSizeClass.Medium -> 2
                        WindowWidthSizeClass.Expanded -> 3
                        else -> 1
                    }
                    // group the elements in list
                    val groups = list.chunked(groupCount)
                    for (group in groups) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (item in group) {
                                PlatformCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                        .clickable { showingCreationDialog = item }
                                ) {
                                    BasicText(
                                        text = item.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                            .padding(top = 8.dp),
                                        style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                    )
                                    BasicText(
                                        text = (item.brand ?: "") + " " + (item.model ?: ""),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        style = getPlatformTextStyles().label
                                    )
                                    BasicText(
                                        text = item.description ?: "",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                            .padding(bottom = 8.dp),
                                        style = getPlatformTextStyles().body
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemsCard(
    items: List<ItemD>?,
    itemTypes: List<ItemTypeD>?,
    isCreating: Boolean,
    onCreateRequested: (ItemD, onCreate: () -> Unit) -> Unit,
    allBookings: List<LendingD>?
) {
    var showingCreationDialog: ItemD? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.items_title,
        isCreating = isCreating,
        onCreateRequested = onCreateRequested,
        onDismissRequested = { if (!isCreating) showingCreationDialog = null }
    ) { data ->
        PlatformDropdown(
            value = data.health,
            onValueChange = { showingCreationDialog = data.copy(health = it) },
            options = ItemHealth.entries,
            label = stringResource(Res.string.items_health),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            toString = { it?.name ?: "" }
        )
        PlatformTextArea(
            value = data.notes ?: "",
            onValueChange = { showingCreationDialog = data.copy(notes = it.takeIf(String::isNotBlank)) },
            label = stringResource(Res.string.items_notes),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating
        )
        PlatformDropdown(
            value = data.typeId?.let { typeId -> itemTypes?.find { it.id == typeId } },
            onValueChange = { showingCreationDialog = data.copy(typeId = it.id) },
            options = itemTypes ?: emptyList(),
            label = stringResource(Res.string.items_type),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            enabled = !isCreating,
            toString = { it?.title ?: "" }
        )
    }

    var showingDetailsDialog: ItemD? by remember { mutableStateOf(null) }
    showingDetailsDialog?.let { data ->
        PlatformDialog(
            onDismissRequest = { showingDetailsDialog = null }
        ) {
            BasicText(
                text = stringResource(Res.string.items_details_title),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            BasicText(
                text = stringResource(Res.string.items_health_value, stringResource(data.health())),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            BasicText(
                text = stringResource(Res.string.items_notes_value, "\n${data.notes ?: "N/A"}"),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            BasicText(
                text = stringResource(
                    Res.string.items_type_value,
                    itemTypes?.find { it.id == data.typeId }?.title ?: "N/A"
                ),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            allBookings
                // Filter bookings that contain this item
                ?.filter { it.itemIds?.contains(data.id) == true }
                // Filter bookings that have not been completed (still not returned)
                ?.filter { it.returnedAt == null }
                ?.let { bookings ->
                    val taken = bookings.find { it.takenAt != null }
                    if (taken != null) {
                        val at = Instant.fromEpochMilliseconds(taken.takenAt!!)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date

                        BasicText(
                            text = stringResource(Res.string.items_details_taken, taken.userId ?: "N/A", at.toString()),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                    val future = bookings.filter { it.id != taken?.id }
                    if (future.isNotEmpty()) {
                        BasicText(
                            text = stringResource(Res.string.items_details_future_bookings),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                        )
                        for (booking in future) {
                            val from = Instant.fromEpochMilliseconds(booking.from!!)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date

                            BasicText(
                                text = "· " + stringResource(
                                    if (booking.confirmed)
                                        Res.string.items_details_future_booking
                                    else
                                        Res.string.items_details_future_booking_not_confirmed,
                                    from.toString(),
                                    booking.userId ?: "N/A"
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        BasicText(
                            text = stringResource(Res.string.items_details_not_booked),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                }
        }
    }

    PlatformCard(
        title = stringResource(Res.string.items_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add)) { showingCreationDialog = ItemD() },
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = items to itemTypes,
            modifier = Modifier.fillMaxWidth()
        ) { (list, types) ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (list == null || types == null) {
                    PlatformLoadingIndicator(large = false)
                } else if (list.isEmpty()) {
                    BasicText(
                        text = stringResource(Res.string.types_empty),
                        style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    for (item in list) {
                        val type = types.find { it.id == item.typeId } ?: continue
                        PlatformCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { showingDetailsDialog = item }
                        ) {
                            BasicText(
                                text = "#${item.id} - ${type.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                            BasicText(
                                text = stringResource(item.health.localizedName()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label
                            )

                            BasicText(
                                text = "Edit",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .padding(vertical = 8.dp)
                                    .clickable { showingCreationDialog = item },
                                style = getPlatformTextStyles().label.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingsCard(
    bookings: List<LendingD>?,
    items: List<ItemD>?,
    itemTypes: List<ItemTypeD>?,
    isUpdatingBooking: Boolean,
    onConfirmBookingRequested: (LendingD, () -> Unit) -> Unit,
    onMarkAsTakenRequested: (LendingD, () -> Unit) -> Unit,
    onMarkAsReturnedRequested: (LendingD, () -> Unit) -> Unit
) {
    var confirmBooking by remember { mutableStateOf<LendingD?>(null) }
    confirmBooking?.let { booking ->
        PlatformDialog(
            onDismissRequest = { confirmBooking = null }
        ) {
            BasicText(
                text = stringResource(if (booking.confirmed) Res.string.bookings_info else Res.string.bookings_confirm),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            BasicText(
                text = stringResource(Res.string.bookings_made_by, booking.userId ?: "N/A"),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            BasicText(
                text = stringResource(Res.string.bookings_from, booking.fromDate().toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            BasicText(
                text = stringResource(Res.string.bookings_to, booking.toDate().toString()),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )

            if (booking.confirmed) {
                if (booking.takenAt == null) {
                    BasicText(
                        text = stringResource(Res.string.bookings_not_taken),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                } else {
                    BasicText(
                        text = stringResource(Res.string.bookings_taken_at, booking.takenDate().toString()),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    if (booking.returnedAt == null) {
                        BasicText(
                            text = stringResource(Res.string.bookings_not_returned),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    } else {
                        BasicText(
                            text = stringResource(Res.string.bookings_returned_at, booking.returnedDate().toString()),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            BasicText(
                text = stringResource(Res.string.bookings_items),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
            )
            val itemsAndTypes = remember(booking.itemIds) {
                booking.itemIds
                    ?.mapNotNull { itemId -> items?.find { it.id == itemId } }
                    ?.associateWith { item -> itemTypes?.find { it.id == item.typeId }!! }
                    ?: emptyMap()
            }
            for ((item, type) in itemsAndTypes) {
                BasicText(
                    text = "· ${type.title} (#${item.id})",
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }

            if (!booking.confirmed) {
                PlatformButton(
                    text = stringResource(Res.string.confirm),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onConfirmBookingRequested(booking) { confirmBooking = null }
                }
            } else if (booking.takenAt == null) {
                PlatformButton(
                    text = stringResource(Res.string.bookings_mark_taken),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onMarkAsTakenRequested(booking) { confirmBooking = null }
                }
            } else if (booking.returnedAt == null) {
                PlatformButton(
                    text = stringResource(Res.string.bookings_mark_returned),
                    modifier = Modifier.align(Alignment.End).padding(8.dp).padding(top = 8.dp),
                    enabled = !isUpdatingBooking
                ) {
                    onMarkAsReturnedRequested(booking) { confirmBooking = null }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    var hideComplete by remember { mutableStateOf(true) }

    PlatformCard(
        title = stringResource(Res.string.bookings_list),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AnimatedContent(
            targetState = bookings,
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (list == null) {
                    PlatformLoadingIndicator(large = false)
                } else if (list.isEmpty()) {
                    BasicText(
                        text = stringResource(Res.string.bookings_empty),
                        style = getPlatformTextStyles().label.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    PlatformCheckbox(
                        checked = hideComplete,
                        onCheckedChanged = { hideComplete = it },
                        label = stringResource(Res.string.bookings_hide_complete),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )

                    val filteredList = list.filter { booking ->
                        if (hideComplete) {
                            booking.returnedAt == null
                        } else {
                            true
                        }
                    }
                    for (item in filteredList) {
                        PlatformCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { confirmBooking = item }
                        ) {
                            BasicText(
                                text = item.userId ?: "N/A",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                            BasicText(
                                text = "${item.itemIds?.size} items",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )

                            if (!item.confirmed) {
                                BasicText(
                                    text = stringResource(Res.string.bookings_pending),
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else if (item.takenAt == null) {
                                BasicText(
                                    text = stringResource(Res.string.bookings_not_taken),
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else if (item.returnedAt == null) {
                                BasicText(
                                    text = stringResource(Res.string.bookings_not_returned),
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                BasicText(
                                    text = stringResource(Res.string.bookings_returned),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
