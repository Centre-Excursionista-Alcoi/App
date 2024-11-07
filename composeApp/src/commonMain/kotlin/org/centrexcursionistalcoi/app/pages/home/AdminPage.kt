package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
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
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.PlatformTextArea
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.SectionD
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdminPage(
    isCreatingSection: Boolean,
    sections: List<SectionD>?,
    onCreateSectionRequested: (SectionD, onCreate: () -> Unit) -> Unit,
    itemTypes: List<ItemTypeD>?,
    isCreatingType: Boolean,
    onCreateTypeRequested: (ItemTypeD, onCreate: () -> Unit) -> Unit
) {
    SectionsCard(sections, isCreatingSection, onCreateSectionRequested)

    TypesCard(itemTypes, isCreatingType, onCreateTypeRequested)
}

@Composable
fun SectionsCard(
    sections: List<SectionD>?,
    isCreating: Boolean,
    onCreateRequested: (SectionD, onCreate: () -> Unit) -> Unit,
) {
    var showingCreationDialog by remember { mutableStateOf(false) }

    if (showingCreationDialog) {
        PlatformDialog(
            onDismissRequest = { if (!isCreating) showingCreationDialog = false }
        ) {
            var displayName by remember { mutableStateOf("") }

            BasicText(
                text = stringResource(Res.string.sections_create),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            PlatformFormField(
                value = displayName,
                onValueChange = { displayName = it },
                label = stringResource(Res.string.sections_name),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isCreating
            )
            PlatformButton(
                text = stringResource(Res.string.create),
                modifier = Modifier.align(Alignment.End).padding(8.dp),
                enabled = !isCreating
            ) {
                onCreateRequested(
                    SectionD(displayName = displayName)
                ) { showingCreationDialog = false }
            }
        }
    }

    PlatformCard(
        title = stringResource(Res.string.sections_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add), { showingCreationDialog = true }),
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
                        BasicText(text = item.displayName)
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
    var showingCreationDialog by remember { mutableStateOf(false) }

    if (showingCreationDialog) {
        PlatformDialog(
            onDismissRequest = { if (!isCreating) showingCreationDialog = false }
        ) {
            var title by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var brand by remember { mutableStateOf("") }
            var model by remember { mutableStateOf("") }

            BasicText(
                text = stringResource(Res.string.types_create),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            PlatformFormField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(Res.string.types_name),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isCreating
            )
            PlatformTextArea(
                value = description,
                onValueChange = { description = it },
                label = stringResource(Res.string.types_description),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isCreating
            )
            PlatformFormField(
                value = brand,
                onValueChange = { brand = it },
                label = stringResource(Res.string.types_brand),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isCreating
            )
            PlatformFormField(
                value = model,
                onValueChange = { model = it },
                label = stringResource(Res.string.types_model),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                enabled = !isCreating
            )
            PlatformButton(
                text = stringResource(Res.string.create),
                modifier = Modifier.align(Alignment.End).padding(8.dp),
                enabled = !isCreating
            ) {
                onCreateRequested(
                    ItemTypeD(
                        title = title,
                        description = description.takeIf(String::isNotBlank),
                        brand = brand.takeIf(String::isNotBlank),
                        model = model.takeIf(String::isNotBlank)
                    )
                ) { showingCreationDialog = false }
            }
        }
    }

    PlatformCard(
        title = stringResource(Res.string.types_title),
        action = Triple(Icons.Default.Add, stringResource(Res.string.add)) { showingCreationDialog = true },
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
                                    modifier = Modifier.weight(1f).padding(8.dp)
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
