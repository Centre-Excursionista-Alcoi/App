package ui.reusable.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FormCheckboxList(
    states: List<Boolean>,
    onStatesChanged: (List<Boolean>) -> Unit,
    labels: List<@Composable () -> String>,
    modifier: Modifier = Modifier
) {
    labels.forEachIndexed { index, label ->
        val checked = states.getOrNull(index)
        FormCheckbox(
            checked = checked ?: false,
            onCheckedChange = {
                val newState = states.toMutableList().apply { set(index, !(checked ?: false)) }
                onStatesChanged(newState)
            },
            text = label(),
            modifier = modifier
        )
    }
}
