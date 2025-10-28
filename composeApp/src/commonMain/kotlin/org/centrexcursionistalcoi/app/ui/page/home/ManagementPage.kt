package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.dialog.CreateInventoryItemTypeDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    onManageLendingsRequested: () -> Unit,
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(key = "lendings") {
            Button(
                onClick = onManageLendingsRequested
            ) { Text(stringResource(Res.string.management_lendings)) }
        }
        item(key = "departments") {
            DepartmentsCard(departments, onCreateDepartment, onDeleteDepartment)
        }
        item(key = "users") {
            UsersCard(users, onPromote)
        }
        item(key = "items") {
            InventoryItemTypesCard(
                inventoryItemTypes,
                inventoryItems,
                onCreateInventoryItemType,
                onClickInventoryItemType,
            )
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDelete: (Department) -> Job,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateDepartmentDialog(onCreate) { creating = false }
    }

    ListCard(
        list = departments,
        titleResource = Res.string.management_departments,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.displayName },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onDelete = onDelete
    )
}

@Composable
fun InventoryItemTypesCard(
    types: List<InventoryItemType>?,
    items: List<ReferencedInventoryItem>?,
    onCreate: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onClick: (InventoryItemType) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateInventoryItemTypeDialog(onCreate) { creating = false }
    }

    val groupedItems = remember(items, types) {
        items?.groupBy { it.type }?.toList()
    }
    ListCard(
        list = groupedItems,
        titleResource = Res.string.management_inventory_item_types,
        emptyTextResource = Res.string.management_no_item_types,
        displayName = { (type, items) -> "${type.displayName} (${items.size})" },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onClick = { (type) -> onClick(type) },
        sharedContentStateKey = "inventory_item_type",
    )
}

@Composable
fun UsersCard(users: List<UserData>?, onPromote: (UserData) -> Job) {
    var promotingUser by remember { mutableStateOf<UserData?>(null) }
    promotingUser?.let { user ->
        var isPromoting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isPromoting) promotingUser = null },
            title = { Text(stringResource(Res.string.management_promote_user_title)) },
            text = { Text(stringResource(Res.string.management_promote_user_confirmation, user.username)) },
            confirmButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = {
                        isPromoting = true
                        onPromote(user).invokeOnCompletion {
                            isPromoting = false
                            promotingUser = null
                        }
                    }
                ) { Text(stringResource(Res.string.management_promote_user)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = { if (!isPromoting) promotingUser = null }
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    ListCard(
        list = users,
        titleResource = Res.string.management_users,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.username },
        trailingContent = { if (it.isAdmin()) Badge { Text(stringResource(Res.string.admin)) } },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        actions = { user ->
            listOfNotNull(
                IconAction(
                    icon = Icons.Default.AddModerator,
                    onClick = { promotingUser = user },
                    contentDescription = stringResource(Res.string.management_promote_user),
                ).takeUnless { user.isAdmin() }
            )
        }
    )
}

@Composable
fun CreateDepartmentDialog(
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image
    ) { file -> image = file }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create department") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                    onClick = { imagePicker.launch() }
                ) {
                    image?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    } ?: Text(
                        "Select image (optional)",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName, image).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}
