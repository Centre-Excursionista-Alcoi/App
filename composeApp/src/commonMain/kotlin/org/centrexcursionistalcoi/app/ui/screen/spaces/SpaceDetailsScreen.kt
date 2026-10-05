package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpaceDetailsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SpaceDetailsScreen(
    spaceId: Uuid,
    onBook: () -> Unit,
    onBack: () -> Unit,
    model: SpaceDetailsViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val space by model.space.collectAsState()
    val keys by model.keys.collectAsState()
    val occupancy by model.occupancy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(space?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        val space = space
        if (space == null) {
            LoadingBox()
            return@Scaffold
        }
        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            item("description") {
                Text(space.description, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (space.isClosed) {
                item("closed") {
                    Text(
                        text = space.closedReason?.let { stringResource(Res.string.spaces_closed_reason, it) }
                            ?: stringResource(Res.string.spaces_closed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            item("prices_title") {
                Text(stringResource(Res.string.spaces_prices), style = MaterialTheme.typography.titleMedium)
            }
            if (space.prices.isEmpty()) {
                item("prices_free") { Text(stringResource(Res.string.spaces_price_free)) }
            }
            items(space.prices, key = { "price_${it.category}" }) { price ->
                Text(
                    stringResource(
                        Res.string.spaces_price_line,
                        price.category.label(),
                        formatPrice(price.price),
                        price.unit.label(),
                    )
                )
            }
            if (!keys.isNullOrEmpty()) {
                item("keys_title") {
                    Text(
                        stringResource(Res.string.spaces_keys),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(keys.orEmpty(), key = { "key_${it.id}" }) { key ->
                    Text(stringResource(Res.string.spaces_key_line, key.name, key.maxQuantity))
                }
            }
            space.conditionsOfUse?.let { conditions ->
                item("conditions") {
                    Text(
                        stringResource(Res.string.spaces_conditions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(conditions)
                }
            }
            item("occupancy_title") {
                Text(
                    stringResource(Res.string.spaces_occupied),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (occupancy.isNullOrEmpty()) {
                item("occupancy_none") { Text(stringResource(Res.string.spaces_occupied_none)) }
            }
            items(occupancy.orEmpty(), key = { "occupied_${it.checkIn}_${it.checkOut}" }) { range ->
                Text(stringResource(Res.string.spaces_occupied_range, range.checkIn.formatted(), range.checkOut.formatted()))
            }
            item("book") {
                Button(
                    onClick = onBook,
                    enabled = !(space.isClosed && space.closedUntil == null),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                ) {
                    Text(stringResource(if (space.isClosed && space.closedUntil == null) Res.string.spaces_book_closed else Res.string.spaces_book))
                }
            }
        }
    }
}
