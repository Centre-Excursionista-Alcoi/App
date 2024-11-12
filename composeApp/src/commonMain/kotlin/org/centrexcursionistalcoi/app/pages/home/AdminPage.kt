package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
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
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.composition.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformDropdown
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.DatabaseData
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.centrexcursionistalcoi.app.server.response.data.enumeration.ItemHealth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdminPage(
    isCreatingSection: Boolean,
    sections: List<SectionD>?,
    onSectionOperation: (SectionD, onCreate: () -> Unit) -> Unit,
    itemTypes: List<ItemTypeD>?,
    isCreatingType: Boolean,
    onTypeOperation: (ItemTypeD, onCreate: () -> Unit) -> Unit,
    items: List<ItemD>?,
    isCreatingItem: Boolean,
    onItemOperation: (ItemD, onCreate: () -> Unit) -> Unit
) {
    SectionsCard(sections, isCreatingSection, onSectionOperation)

    TypesCard(itemTypes, isCreatingType, onTypeOperation)

    ItemsCard(items, itemTypes, isCreatingItem, onItemOperation)
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

@Composable
fun TypesCard(
    itemTypes: List<ItemTypeD>?,
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
    onCreateRequested: (ItemD, onCreate: () -> Unit) -> Unit
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
        PlatformFormField(
            value = data.amount?.toString() ?: "",
            onValueChange = { value ->
                if (value.isBlank()) {
                    showingCreationDialog = data.copy(amount = null)
                } else {
                    val num = value.toIntOrNull() ?: return@PlatformFormField
                    showingCreationDialog = data.copy(amount = num)
                }
            },
            label = stringResource(Res.string.items_amount),
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

    PlatformCard(
        title = stringResource(Res.string.types_title),
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
                                .clickable { showingCreationDialog = item }
                        ) {
                            BasicText(
                                text = type.title + " x" + item.amount,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                            BasicText(
                                text = item.health.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(top = 8.dp),
                                style = getPlatformTextStyles().label
                            )
                        }
                    }
                }
            }
        }
    }
}
