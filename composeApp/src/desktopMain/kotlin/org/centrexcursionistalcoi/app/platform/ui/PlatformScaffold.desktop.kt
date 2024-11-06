package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PlatformScaffold(
    title: String?,
    actions: List<Triple<ImageVector, String, () -> Unit>>,
    navigationBar: (@Composable () -> Unit)?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                PlatformButton(stringResource(Res.string.back), onClick = it)
            }
            AnimatedContent(
                targetState = title,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            ) { state ->
                state?.let {
                    BasicText(
                        text = it,
                        style = getPlatformTextStyles().heading
                    )
                }
            }
            for ((_, text, action) in actions) {
                PlatformButton(text, onClick = action)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content(PaddingValues(0.dp))
        }

        navigationBar?.invoke()
    }
}
