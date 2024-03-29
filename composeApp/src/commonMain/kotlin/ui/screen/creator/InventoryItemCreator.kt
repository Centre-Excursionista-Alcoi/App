package ui.screen.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import platform.NavigationInformation
import screenmodel.InventoryItemModel
import ui.reusable.form.FormColumn
import ui.reusable.form.FormField
import ui.reusable.form.FormMultilineField
import ui.reusable.navigation.AppBarBackButton
import ui.screen.BaseScreen
import ui.screen.MainScreen

class InventoryItemCreator : BaseScreen({ "Create New Item" }, true) {
    @Composable
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun ScreenContent() {
        val navigator = LocalNavigator.currentOrThrow

        val model = rememberScreenModel { InventoryItemModel() }

        var displayName by remember { mutableStateOf<TextFieldValue?>(null) }
        var description by remember { mutableStateOf<TextFieldValue?>(null) }

        val descriptionFocusRequester = remember { FocusRequester() }

        fun create() {
            val displayNameValue = displayName?.text ?: return
            val descriptionValue = description?.text

            val task = model.create(displayNameValue, descriptionValue)
            task.invokeOnCompletion {
                val exception = task.getCompletionExceptionOrNull()
                if (exception == null) {
                    val result = task.getCompleted()
                    println("Inserted new item with result: ${result.data}")
                    navigator.pop()
                    navigator.items
                        .filterIsInstance<MainScreen>()
                        .firstOrNull()
                        ?.model
                        ?.loadInventoryItems()
                        ?.start()
                } else {
                    throw exception
                }
            }
        }

        Scaffold(
            topBar = { TopBar() }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    FormField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Display Name",
                        capitalization = KeyboardCapitalization.Sentences,
                        modifier = Modifier.fillMaxWidth(),
                        onSubmit = ::create,
                        nextFocusRequester = descriptionFocusRequester
                    )
                    FormMultilineField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description",
                        capitalization = KeyboardCapitalization.Sentences,
                        modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocusRequester)
                    )
                }
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = ::create,
                        enabled = !displayName?.text.isNullOrBlank(),
                        modifier = Modifier.padding(vertical = 8.dp).padding(end = 12.dp)
                    ) {
                        Text("Store")
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopBar() {
        if (!NavigationInformation.hasNavigationBar) {
            TopAppBar(
                title = { Text("Create New Item") },
                navigationIcon = {
                    AppBarBackButton()
                }
            )
        }
    }
}
