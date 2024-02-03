package ui.state

import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.MutableStateFlow

object SharedApplicationState {
    val title = MutableStateFlow<String?>(null)

    val canGoBack = MutableStateFlow(false)

    val navigator = MutableStateFlow<Navigator?>(null)
}
