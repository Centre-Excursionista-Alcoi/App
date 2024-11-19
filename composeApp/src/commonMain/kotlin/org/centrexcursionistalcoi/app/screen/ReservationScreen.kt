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
import androidx.compose.runtime.remember
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
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Reservation
import org.centrexcursionistalcoi.app.viewmodel.ReservationViewModel
import org.jetbrains.compose.resources.stringResource

object ReservationScreen : Screen<Reservation, ReservationViewModel>(::ReservationViewModel) {
    @Composable
    override fun Content(viewModel: ReservationViewModel) {
        val userData by viewModel.userData.collectAsState()
        val itemsState by viewModel.items.collectAsState()
        val typesState by viewModel.types.collectAsState()

        val from = route.fromDate()
        val to = route.toDate()
        val itemsId = route.selectedItemsSet()

        val navController = LocalNavController.current

        LaunchedEffect(Unit) {
            viewModel.load(itemsId)
        }

        PlatformScaffold(
            onBack = { navController.popBackStack() },
            title = stringResource(Res.string.reservation_title_draft),
            actions = listOf(
                Triple(Icons.AutoMirrored.Filled.ArrowRight, stringResource(Res.string.confirm)) {
                    viewModel.confirm(from, to, itemsId) {
                        navController.popBackStack()
                    }
                }
            )
        ) { paddingValues ->
            userData?.let { user ->
                itemsState?.let { items ->
                    typesState?.let { types ->
                        Content(paddingValues, user, from, to, items, types)
                    } ?: run {
                        PlatformLoadingIndicator(modifier = Modifier.fillMaxSize())
                    }
                } ?: run {
                    PlatformLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
            } ?: run {
                PlatformLoadingIndicator(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun Content(
        paddingValues: PaddingValues,
        userData: UserD,
        from: LocalDate,
        to: LocalDate,
        selectedItems: List<ItemD>,
        types: List<ItemTypeD>
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

            /*BasicText(
                text = stringResource(Res.string.reservation_spaces),
                style = getPlatformTextStyles().titleRegular,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            BasicText(
                text = stringResource(Res.string.reservation_no_spaces),
                style = getPlatformTextStyles().titleRegular.copy(fontSize = 20.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )*/
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
}
