package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.animation.sharedBounds
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.viewmodel.InventoryItemTypeDetailsScreenModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemTypeDetailsScreen(
    typeDisplayName: String,
    typeId: Uuid,
    model: InventoryItemTypeDetailsScreenModel = viewModel { InventoryItemTypeDetailsScreenModel(typeId) },
    onBack: () -> Unit
) {
    val type by model.type.collectAsState()

    InventoryItemTypeDetailsScreen(
        typeDisplayName = typeDisplayName,
        typeId = typeId,
        type = type,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemTypeDetailsScreen(
    typeDisplayName: String,
    typeId: Uuid,
    type: InventoryItemType?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {
                    Text(
                        text = typeDisplayName,
                        modifier = Modifier.sharedBounds("type-${typeId}-display-name"),
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumnWidthWrapper(
            modifier = Modifier.padding(paddingValues).fillMaxWidth()
        ) {
            item("image") {
                val imageFile by type.rememberImageFile()
                AsyncByteImage(
                    bytes = imageFile,
                    contentDescription = typeDisplayName,
                    modifier = Modifier.fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(bottom = 8.dp)
                        .sharedBounds("type-${typeId}-image")
                )
            }

            val description = type?.description
            if (!description.isNullOrBlank()) item("description") {
                OutlinedCard(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp).padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = stringResource(Res.string.form_description),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = stringResource(Res.string.form_description),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
