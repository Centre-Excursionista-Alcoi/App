package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Reservation
import org.centrexcursionistalcoi.app.utils.toStringWithDecimals
import org.centrexcursionistalcoi.app.viewmodel.ReservationViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

object ReservationScreen : Screen<Reservation, ReservationViewModel>(::ReservationViewModel) {
    @Composable
    override fun Content(viewModel: ReservationViewModel) {
        val navController = LocalNavController.current

        if (!route.isCorrect()) {
            navController.popBackStack()
            return
        }

        val userData by viewModel.userData.collectAsState()
        val dates by viewModel.dates.collectAsState()
        val items by viewModel.items.collectAsState()
        val types by viewModel.types.collectAsState()
        val space by viewModel.space.collectAsState()
        val itemLending by viewModel.itemLending.collectAsState()
        val spaceBooking by viewModel.spaceBooking.collectAsState()

        if (route.isDraft()) {
            val itemsIds = route.selectedItemsSet()

            LaunchedEffect(Unit) {
                viewModel.load(route.fromDate()!!, route.toDate()!!, itemsIds, route.selectedSpaceId)
            }
        } else {
            LaunchedEffect(Unit) {
                viewModel.load(route.lendingId, route.spaceBookingId)
            }
        }

        var cancellingReservation by remember { mutableStateOf(false) }
        if (cancellingReservation) {
            CancelBookingConfirmationDialog(
                onDismissRequested = { cancellingReservation = false },
                onCancellationRequested = {
                    viewModel.cancelBooking {
                        cancellingReservation = false
                        navController.popBackStack()
                    }
                }
            )
        }

        PlatformScaffold(
            onBack = { navController.popBackStack() },
            title = stringResource(
                if (route.isDraft()) Res.string.reservation_draft_title else Res.string.reservation_title
            ),
            actions = listOfNotNull(
                Triple(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.confirm)) {
                    viewModel.confirm(
                        route.fromDate()!!,
                        route.toDate()!!,
                        route.selectedItemsSet(),
                        route.selectedSpaceId
                    ) {
                        navController.popBackStack()
                    }
                }.takeIf { route.isDraft() },
                Triple(Icons.Default.EventBusy, stringResource(Res.string.cancel)) {
                    cancellingReservation = true
                }.takeIf {
                    !route.isDraft() && (itemLending ?: spaceBooking)?.run { takenAt == null } == true
                },
            )
        ) { paddingValues ->
            LoadingContent(
                paddingValues,
                route.isDraft(),
                userData,
                dates,
                items,
                types,
                space,
                itemLending,
                spaceBooking
            )
        }
    }

    @Composable
    private fun LoadingContent(
        paddingValues: PaddingValues,
        isDraft: Boolean,
        userData: UserD?,
        dates: ClosedRange<LocalDate>?,
        selectedItems: List<ItemD>?,
        types: List<ItemTypeD>?,
        space: SpaceD?,
        itemLending: ItemLendingD?,
        spaceBooking: SpaceBookingD?
    ) {
        if (userData == null || dates == null || selectedItems == null || types == null) {
            PlatformLoadingIndicator(modifier = Modifier.fillMaxSize())
        } else {
            val (from, to) = dates.start to dates.endInclusive

            Content(paddingValues, isDraft, userData, from, to, selectedItems, types, space, itemLending, spaceBooking)
        }
    }

    @Composable
    private fun Content(
        paddingValues: PaddingValues,
        isDraft: Boolean,
        userData: UserD,
        from: LocalDate,
        to: LocalDate,
        selectedItems: List<ItemD>,
        types: List<ItemTypeD>,
        space: SpaceD?,
        itemLending: ItemLendingD?,
        spaceBooking: SpaceBookingD?
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                AppText(
                    text = stringResource(Res.string.reservation_from, from.toString()),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                AppText(
                    text = stringResource(Res.string.reservation_to, to.toString()),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            AppText(
                text = stringResource(
                    if (isDraft) Res.string.reservation_draft_as else Res.string.reservation_as,
                    userData.name,
                    (to - from).days
                ),
                style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth()
            )

            if ((itemLending ?: spaceBooking)?.confirmed == false) {
                AppText(
                    text = stringResource(Res.string.bookings_pending_confirmation),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp, color = Color.Red),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            if (selectedItems.isNotEmpty()) {
                AppText(
                    text = stringResource(Res.string.reservation_items),
                    style = getPlatformTextStyles().titleRegular,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                ItemsDisplay(selectedItems, types)
            }

            if (space != null) {
                AppText(
                    text = stringResource(Res.string.reservation_space),
                    style = getPlatformTextStyles().titleRegular,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                SpaceDisplay(
                    space = space,
                    days = (to - from).days
                )
            }
        }
    }

    @Composable
    private fun ItemsDisplay(selectedItems: List<ItemD>, types: List<ItemTypeD>) {
        val groupedItems = remember(selectedItems) {
            selectedItems
                .groupBy { it.typeId }
                .mapKeys { (typeId, _) -> types.find { it.id == typeId } }
        }
        for ((type, items) in groupedItems) {
            type ?: continue

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                AppText(
                    text = items.size.toString(),
                    style = getPlatformTextStyles().heading.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(8.dp)
                )
                Column(
                    modifier = Modifier.weight(1f).padding(8.dp)
                ) {
                    AppText(
                        text = type.title,
                        style = getPlatformTextStyles().heading.copy(fontSize = 18.sp)
                    )
                    type.brand?.let {
                        AppText(
                            text = it,
                            style = getPlatformTextStyles().heading
                        )
                    }
                    type.model?.let {
                        AppText(
                            text = it,
                            style = getPlatformTextStyles().heading
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SpaceDisplay(space: SpaceD, days: Int) {
        AppText(
            text = space.name,
            style = getPlatformTextStyles().heading.copy(fontSize = 18.sp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )

        val memberPrice = space.memberPrice
        val externalPrice = space.externalPrice
        if (memberPrice != null || externalPrice != null) {
            var membersCount by remember { mutableStateOf("") }
            var externalsCount by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                PlatformFormField(
                    value = membersCount,
                    onValueChange = { value ->
                        val num = value.toUIntOrNull()
                        if (num != null) {
                            membersCount = value
                        }
                    },
                    label = stringResource(Res.string.reservation_space_members),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                PlatformFormField(
                    value = externalsCount,
                    onValueChange = { value ->
                        val num = value.toUIntOrNull()
                        if (num != null) {
                            externalsCount = value
                        }
                    },
                    label = stringResource(Res.string.reservation_space_non_members),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            if (memberPrice != null) {
                val count = membersCount.toIntOrNull() ?: 0
                AppText(
                    text = pluralStringResource(
                        Res.plurals.reservation_space_price_days,
                        days,
                        pluralStringResource(Res.plurals.reservation_space_price_members_count, count, count),
                        memberPrice.amount.toStringWithDecimals(2) + "€",
                        days,
                        (memberPrice.amount * count * days).toStringWithDecimals(2) + "€"
                    ),
                    style = getPlatformTextStyles().heading.copy(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
            if (externalPrice != null) {
                val count = externalsCount.toIntOrNull() ?: 0
                AppText(
                    text = pluralStringResource(
                        Res.plurals.reservation_space_price_days,
                        days,
                        pluralStringResource(Res.plurals.reservation_space_price_members_count, count, count),
                        externalPrice.amount.toStringWithDecimals(2) + "€",
                        days,
                        (externalPrice.amount * count * days).toStringWithDecimals(2) + "€"
                    ),
                    style = getPlatformTextStyles().heading.copy(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
            AppText(
                text = stringResource(Res.string.reservation_space_price_calculation),
                style = getPlatformTextStyles().heading.copy(fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }

    @Composable
    fun CancelBookingConfirmationDialog(
        onDismissRequested: () -> Unit,
        onCancellationRequested: () -> Unit
    ) {
        PlatformDialog(
            onDismissRequested,
            actions = {
                NeutralButton(
                    text = stringResource(Res.string.dismiss),
                    onClick = onDismissRequested
                )
                DestructiveButton(
                    text = stringResource(Res.string.confirm),
                    onClick = onCancellationRequested
                )
            }
        ) {
            AppText(
                text = stringResource(Res.string.reservation_cancel_confirmation_title),
                style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.reservation_cancel_confirmation_message),
                style = getPlatformTextStyles().body,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }
}
