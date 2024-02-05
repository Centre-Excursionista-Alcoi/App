package ui.reusable.form

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FormColumn(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    maxWidth: Dp = 600.dp,
    onSubmit: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onKeyEvent { event ->
                when {
                    event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                        onSubmit()
                        true
                    }
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(modifier)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().then(contentModifier)
            ) {
                content()
            }
        }
    }
}
