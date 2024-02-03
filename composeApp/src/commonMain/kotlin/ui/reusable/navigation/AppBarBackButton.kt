package ui.reusable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
@Suppress("UnusedReceiverParameter")
fun Screen.AppBarBackButton() {
    val navigator = LocalNavigator.currentOrThrow
    IconButton(
        onClick = navigator::popUntilRoot
    ) { Icon(Icons.Rounded.ChevronLeft, null) }
}
