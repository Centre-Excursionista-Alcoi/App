package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.rememberPlainPositionProvider
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.enumeration.ItemHealth
import org.centrexcursionistalcoi.app.data.health
import org.centrexcursionistalcoi.app.data.localizedName
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemsCard(
    items: List<ItemD>?,
    itemTypes: List<ItemTypeD>?,
    isCreating: Boolean,
    onCreateRequested: (ItemD, onCreate: () -> Unit) -> Unit,
    allBookings: List<ItemLendingD>?
) {
    var showingCreationDialog: ItemD? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.items_title,
        isCreating = isCreating,
        isEnabled = ItemD::validate,
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
            AppText(
                text = stringResource(Res.string.items_details_title),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.items_health_value, stringResource(data.health())),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.items_notes_value, "\n${data.notes ?: "N/A"}"),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
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
                        val at = taken.takenAt

                        AppText(
                            text = stringResource(Res.string.items_details_taken, taken.userId ?: "N/A", at.toString()),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                    val future = bookings.filter { it.id != taken?.id }
                    if (future.isNotEmpty()) {
                        AppText(
                            text = stringResource(Res.string.items_details_future_bookings),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
                        )
                        for (booking in future) {
                            val from = booking.from

                            AppText(
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
                        AppText(
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
                    AppText(
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (color, tooltip) = allBookings
                                    ?.filter { it.itemIds?.contains(item.id) == true }
                                    ?.let { bookings ->
                                        val taken = bookings.find { it.takenAt != null }
                                        if (taken != null) {
                                            Color.Red to stringResource(Res.string.items_details_taken_by, taken.userId ?: "N/A")
                                        } else {
                                            Color.Green to stringResource(Res.string.items_details_not_booked)
                                        }
                                    } ?: (Color.Yellow to stringResource(Res.string.loading))

                                BasicTooltipBox(
                                    positionProvider = rememberPlainPositionProvider(),
                                    state = rememberBasicTooltipState(),
                                    tooltip = { AppText(tooltip) },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                                AppText(
                                    text = "#${item.id} - ${type.title}",
                                    modifier = Modifier.weight(1f),
                                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            AppText(
                                text = stringResource(item.health.localizedName()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label
                            )

                            AppText(
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
