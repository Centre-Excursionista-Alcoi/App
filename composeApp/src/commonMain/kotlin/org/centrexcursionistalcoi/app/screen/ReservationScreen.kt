package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
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
        val userData by viewModel.userData.collectAsState()
        val itemsState by viewModel.items.collectAsState()
        val typesState by viewModel.types.collectAsState()
        val spaceState by viewModel.space.collectAsState()

        val from = route.fromDate()
        val to = route.toDate()
        val itemsIds = route.selectedItemsSet()

        val navController = LocalNavController.current

        LaunchedEffect(Unit) {
            viewModel.load(itemsIds, route.selectedSpaceId)
        }

        PlatformScaffold(
            onBack = { navController.popBackStack() },
            title = stringResource(Res.string.reservation_title_draft),
            actions = listOf(
                Triple(Icons.AutoMirrored.Filled.ArrowRight, stringResource(Res.string.confirm)) {
                    viewModel.confirm(from, to, itemsIds, route.selectedSpaceId) {
                        navController.popBackStack()
                    }
                }
            )
        ) { paddingValues ->
            LoadingContent(paddingValues, userData, from, to, itemsState, typesState, spaceState)
        }
    }

    @Composable
    private fun LoadingContent(
        paddingValues: PaddingValues,
        userData: UserD?,
        from: LocalDate,
        to: LocalDate,
        selectedItems: List<ItemD>?,
        types: List<ItemTypeD>?,
        space: SpaceD?
    ) {
        if (userData == null || selectedItems == null || types == null) {
            PlatformLoadingIndicator(modifier = Modifier.fillMaxSize())
        } else {
            Content(paddingValues, userData, from, to, selectedItems, types, space)
        }
    }

    @Composable
    private fun Content(
        paddingValues: PaddingValues,
        userData: UserD,
        from: LocalDate,
        to: LocalDate,
        selectedItems: List<ItemD>,
        types: List<ItemTypeD>,
        space: SpaceD?
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
                BasicText(
                    text = stringResource(Res.string.reservation_from, from.toString()),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                BasicText(
                    text = stringResource(Res.string.reservation_to, to.toString()),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            BasicText(
                text = stringResource(Res.string.reservation_as, userData.name, (to - from).days),
                style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth(1f)
            )

            BasicText(
                text = stringResource(Res.string.reservation_items),
                style = getPlatformTextStyles().titleRegular,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            if (selectedItems.isEmpty()) {
                BasicText(
                    text = stringResource(Res.string.reservation_no_items),
                    style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            } else {
                ItemsDisplay(selectedItems, types)
            }

            if (space != null) {
                BasicText(
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
                BasicText(
                    text = items.size.toString(),
                    style = getPlatformTextStyles().heading.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(8.dp)
                )
                Column(
                    modifier = Modifier.weight(1f).padding(8.dp)
                ) {
                    BasicText(
                        text = type.title,
                        style = getPlatformTextStyles().heading.copy(fontSize = 18.sp)
                    )
                    type.brand?.let {
                        BasicText(
                            text = it,
                            style = getPlatformTextStyles().heading
                        )
                    }
                    type.model?.let {
                        BasicText(
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
        BasicText(
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
                    label = "Members",
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
                    label = "Non-Members",
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            if (memberPrice != null) {
                val count = membersCount.toIntOrNull() ?: 0
                BasicText(
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
                BasicText(
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
            BasicText(
                text = stringResource(Res.string.reservation_space_price_calculation),
                style = getPlatformTextStyles().heading.copy(fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }
}
