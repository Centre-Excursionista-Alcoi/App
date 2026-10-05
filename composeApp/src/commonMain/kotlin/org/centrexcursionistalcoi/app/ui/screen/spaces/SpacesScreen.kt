package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpacesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun SpacesScreen(
    onSpaceClick: (Uuid) -> Unit,
    onLendingClick: (Uuid) -> Unit,
    onBack: () -> Unit,
    model: SpacesViewModel = koinViewModel(),
) {
    val spaces by model.spaces.collectAsState()
    val lendings by model.myLendings.collectAsState()

    SpacesScreenContent(spaces, lendings, onSpaceClick, onLendingClick, onBack)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SpacesScreenContent(
    spaces: List<Space>?,
    lendings: List<SpaceLending>?,
    onSpaceClick: (Uuid) -> Unit,
    onLendingClick: (Uuid) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.spaces_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        if (spaces == null) {
            LoadingBox()
            return@Scaffold
        }
        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues)) {
            if (spaces.isEmpty()) {
                item("empty") {
                    Text(stringResource(Res.string.spaces_empty), modifier = Modifier.padding(16.dp))
                }
            }
            items(spaces, key = { it.id.toString() }) { space ->
                ListItem(
                    headlineContent = { Text(space.name) },
                    supportingContent = {
                        Column {
                            Text(space.description, maxLines = 2)
                            if (space.isClosed) {
                                Text(
                                    text = stringResource(Res.string.spaces_closed),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onSpaceClick(space.id) },
                )
                HorizontalDivider()
            }
            if (!lendings.isNullOrEmpty()) {
                item("my_lendings_title") {
                    Text(
                        text = stringResource(Res.string.spaces_my_lendings),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
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
        }
    }
}
