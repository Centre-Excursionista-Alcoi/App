package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingStage
import org.centrexcursionistalcoi.app.ui.screen.spaces.formatted
import org.centrexcursionistalcoi.app.ui.screen.spaces.label
import org.centrexcursionistalcoi.app.ui.screen.spaces.stage
import org.centrexcursionistalcoi.app.viewmodel.management.SpaceLendingsManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

/**
 * Every space lending, for the people who manage them: tap one to hand its keys over, take them back and set it paid.
 */
@Composable
fun SpaceLendingsListView(
    onSpaceLendingClick: (Uuid) -> Unit,
    model: SpaceLendingsManagementViewModel = koinViewModel(),
) {
    val lendings by model.lendings.collectAsState()
    val spaces by model.spaces.collectAsState()
    var showFinished by remember { mutableStateOf(false) }

    val lendingsValue = lendings
    if (lendingsValue == null) {
        LoadingBox()
        return
    }
    val visible = lendingsValue
        .filter { showFinished || (it.stage != SpaceLendingStage.PAID && it.stage != SpaceLendingStage.CANCELLED) }
        .sortedByDescending { it.checkIn }

    LazyColumnWidthWrapper(Modifier.fillMaxSize()) {
        item("filter") {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = showFinished,
                    onClick = { showFinished = !showFinished },
                    label = { Text(stringResource(Res.string.space_lending_stage_paid) + " / " + stringResource(Res.string.space_lending_stage_cancelled)) },
                )
            }
        }
        if (visible.isEmpty()) {
            item("empty") {
                Text(stringResource(Res.string.management_space_lendings_empty), modifier = Modifier.padding(16.dp))
            }
        }
        items(visible, key = { it.id.toString() }) { lending ->
            ListItem(
                headlineContent = { Text(spaces.orEmpty().find { it.id == lending.space }?.name.orEmpty()) },
                supportingContent = {
                    Text("${lending.checkIn.formatted()} → ${lending.checkOut.formatted()} · ${lending.stage.label()}")
                },
                modifier = Modifier.clickable { onSpaceLendingClick(lending.id) },
            )
            HorizontalDivider()
        }
    }
}
