package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.component.ImagesCarousel
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.data.localizedName
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.maxGridItemSpan
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformCheckbox
import org.centrexcursionistalcoi.app.platform.ui.PlatformDatePicker
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Reservation
import org.centrexcursionistalcoi.app.state.LocalDateSaver
import org.jetbrains.compose.resources.stringResource

@Composable
fun ColumnScope.ReservationPage(
    itemTypes: List<ItemType>?,
    availableItems: List<Item>?,
    availableSpaces: List<Space>?,
    onLoadAvailabilityRequested: (from: LocalDate, to: LocalDate) -> Unit
) {
    val navController = LocalNavController.current

    var from by rememberSaveable(saver = LocalDateSaver) { mutableStateOf(null) }
    var to by rememberSaveable(saver = LocalDateSaver) { mutableStateOf(null) }
    var selectedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSpaceId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(from, to) {
        snapshotFlow { from to to }
            .distinctUntilChanged()
            .collect { (start, end) ->
                start ?: return@collect
                end ?: return@collect

                if (start > end) {
                    to = null
                    return@collect
                }

                onLoadAvailabilityRequested(start, end)
            }
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxWidth().weight(1f),
        columns = GridCells.Adaptive(200.dp)
    ) {
        if (itemTypes == null) {
            item(key = "loading", span = maxGridItemSpan) {
                PlatformLoadingIndicator(large = false)
            }
        } else {
            item(key = "date-range", span = maxGridItemSpan) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                ) {
                    PlatformDatePicker(
                        value = from,
                        onValueChanged = { from = it },
                        label = stringResource(Res.string.from),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        min = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    )
                    PlatformDatePicker(
                        value = to,
                        onValueChanged = { to = it },
                        label = stringResource(Res.string.to),
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        min = from,
                        enabled = from != null,
                        initialDisplayedDate = from
                    )
                }
            }

            if (from == null || to == null) {
                item(key = "select-date-range", span = maxGridItemSpan) {
                    AppText(
                        text = stringResource(Res.string.home_select_date_range),
                        style = getPlatformTextStyles().heading,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            } else if (availableItems == null || availableSpaces == null) {
                item(key = "loading", span = maxGridItemSpan) {
                    PlatformLoadingIndicator(large = false)
                }
            } else {
                item(key = "summary", span = maxGridItemSpan) {
                    SummaryRow(availableItems.size, selectedItems.size, availableSpaces.size)
                }

                items(
                    key = { "item-${it.first.id}-${it.second?.id}" },
                    items = availableItems.map { item ->
                        item to itemTypes.find { it.id == item.itemTypeId }
                    }
                ) { (item, type) ->
                    if (type == null) return@items
                    val id = item.id
                    ItemCard(
                        item = item,
                        type = type,
                        isSelected = selectedItems.contains(id),
                        toggle = {
                            selectedItems = if (selectedItems.contains(id)) {
                                selectedItems - id
                            } else {
                                selectedItems + id
                            }
                        }
                    )
                }

                items(
                    key = { "space-${it.id}" },
                    items = availableSpaces
                ) { space ->
                    val id = space.id
                    SpaceCard(
                        space = space,
                        isSelected = selectedSpaceId == id,
                        toggle = {
                            selectedSpaceId = if (selectedSpaceId == id) null else id
                        }
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        PlatformButton(
            text = stringResource(Res.string.`continue`),
            enabled = from != null && to != null && (selectedItems.isNotEmpty() || selectedSpaceId != null)
        ) {
            navController.navigate(
                Reservation(
                    from = from ?: return@PlatformButton,
                    to = to ?: return@PlatformButton,
                    selectedItems = selectedItems,
                    selectedSpaceId = selectedSpaceId
                )
            )
        }
    }
}

@Composable
private fun SummaryRow(availableItems: Int, selectedItems: Int, availableSpaces: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 8.dp)
    ) {
        PlatformCard(
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            AppText(
                text = availableItems.toString(),
                style = getPlatformTextStyles().titleRegular.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.home_available_items),
                style = getPlatformTextStyles().heading.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
        PlatformCard(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            AppText(
                text = selectedItems.toString(),
                style = getPlatformTextStyles().titleRegular.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.home_selected_items),
                style = getPlatformTextStyles().heading.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
        PlatformCard(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            AppText(
                text = availableSpaces.toString(),
                style = getPlatformTextStyles().titleRegular.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            AppText(
                text = stringResource(Res.string.home_available_spaces),
                style = getPlatformTextStyles().heading.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    type: ItemType,
    isSelected: Boolean,
    toggle: () -> Unit
) {
    val context = LocalPlatformContext.current

    PlatformCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(role = Role.Checkbox) { toggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            PlatformCheckbox(
                checked = isSelected,
                onCheckedChanged = { toggle() },
                label = null,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                AppText(
                    text = type.title,
                    style = getPlatformTextStyles().heading
                        .copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .padding(horizontal = 8.dp)
                )
                AppText(
                    text = stringResource(item.health.localizedName()),
                    style = getPlatformTextStyles().heading
                        .copy(textAlign = TextAlign.Center, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 8.dp)
                )
                type.image?.let { image ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(image)
                            .build(),
                        contentDescription = type.title,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun SpaceCard(
    space: Space,
    isSelected: Boolean,
    toggle: () -> Unit
) {
    PlatformCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(role = Role.Checkbox) { toggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            PlatformCheckbox(
                checked = isSelected,
                onCheckedChanged = { toggle() },
                label = null,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                AppText(
                    text = space.name,
                    style = getPlatformTextStyles().heading
                        .copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .padding(horizontal = 8.dp)
                )
                ImagesCarousel(
                    images = space.images.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}
