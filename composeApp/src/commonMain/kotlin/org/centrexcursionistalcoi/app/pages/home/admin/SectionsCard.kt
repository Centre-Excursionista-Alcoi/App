package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@Composable
fun SectionsCard(
    sections: List<Section>?,
    isCreating: Boolean,
    onOperationRequested: (Section, onCreate: () -> Unit) -> Unit,
) {
    var showingCreationDialog: Section? by remember { mutableStateOf(null) }
    CreationDialog(
        showingCreationDialog = showingCreationDialog,
        title = Res.string.sections_create,
        isCreating = isCreating,
        isEnabled = Section::validate,
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
        action = Triple(Icons.Default.Add, stringResource(Res.string.add), { showingCreationDialog = Section() }),
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
                        AppText(
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
                            AppText(
                                text = item.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
