package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Home
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.screen.spaces.formatted
import org.centrexcursionistalcoi.app.ui.screen.spaces.label
import org.centrexcursionistalcoi.app.ui.screen.spaces.stage
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/**
 * The tab of [LendingsPage] with the spaces that can be booked, and the user's own space lendings.
 *
 * Spaces can't be mixed with items: while there are items selected a space can't be selected, and while a space is
 * selected ([selectedSpace]) the page hides its tabs, so the selection stays in the spaces.
 */
@Composable
fun SpacesTab(
    spaces: List<Space>,
    lendings: List<SpaceLending>,
    selectedSpace: Space?,
    hasItemsSelected: Boolean,
    onSelect: (Uuid?) -> Unit,
    onDetailsRequested: (Uuid) -> Unit,
    onBookRequested: (Uuid) -> Unit,
    onLendingClick: (Uuid) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("top_spacer") { Column(Modifier.padding(top = 8.dp)) {} }
        if (hasItemsSelected) {
            item("blocked") {
                CardWithIcon(
                    title = stringResource(Res.string.spaces_tab),
                    message = stringResource(Res.string.spaces_blocked_by_items),
                    icon = MaterialSymbols.Home,
                )
            }
        } else if (selectedSpace != null) {
            item("selected_hint") {
                CardWithIcon(
                    title = selectedSpace.name,
                    message = stringResource(Res.string.spaces_selected_hint),
                    icon = MaterialSymbols.Home,
                )
            }
        }
        if (spaces.isEmpty()) {
            item("empty") { Text(stringResource(Res.string.spaces_empty), modifier = Modifier.padding(vertical = 16.dp)) }
        }
        // Once one is selected, only that one is shown
        items(spaces.filter { selectedSpace == null || it.id == selectedSpace.id }, key = { it.id.toString() }) { space ->
            val isSelected = selectedSpace?.id == space.id
            val isClosed = space.isClosed && space.closedUntil == null
            OutlinedCard(
                colors = if (isSelected) {
                    CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    CardDefaults.outlinedCardColors()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(MaterialSymbols.Home, null, modifier = Modifier.padding(end = 8.dp))
                        Text(space.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(space.description, maxLines = 3, modifier = Modifier.padding(vertical = 4.dp))
                    if (space.isClosed) {
                        Text(
                            text = space.closedReason?.let { stringResource(Res.string.spaces_closed_reason, it) }
                                ?: stringResource(Res.string.spaces_closed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { onDetailsRequested(space.id) }) {
                            Text(stringResource(Res.string.spaces_details))
                        }
                        if (isSelected) {
                            Button(onClick = { onBookRequested(space.id) }, enabled = !isClosed) {
                                Text(stringResource(Res.string.spaces_book))
                            }
                            TextButton(onClick = { onSelect(null) }) { Text(stringResource(Res.string.spaces_deselect)) }
                        } else {
                            Button(onClick = { onSelect(space.id) }, enabled = !hasItemsSelected && !isClosed) {
                                Text(stringResource(Res.string.spaces_select))
                            }
                        }
                    }
                }
            }
        }
        if (selectedSpace == null && lendings.isNotEmpty()) {
            item("my_lendings_title") {
                Text(
                    text = stringResource(Res.string.spaces_my_lendings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(lendings, key = { "lending_${it.id}" }) { lending ->
                val space = spaces.find { it.id == lending.space }
                ListItem(
                    headlineContent = { Text(space?.name.orEmpty()) },
                    supportingContent = {
                        Text("${lending.checkIn.formatted()} → ${lending.checkOut.formatted()} · ${lending.stage.label()}")
                    },
                    modifier = Modifier.clickable { onLendingClick(lending.id) },
                )
                HorizontalDivider()
            }
        }
        item("bottom_spacer") { Column(Modifier.padding(bottom = 56.dp)) {} }
    }
}
