package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabrieldrn.carbon.foundation.color.containerBackground

@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    actions: @Composable PlatformDialogContext.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .containerBackground()
                .verticalScroll(rememberScrollState())
        ) {
            content()

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                actions(
                    object : PlatformDialogContext() {
                        override fun dismiss() {
                            onDismissRequest()
                        }

                        override fun Modifier.align(alignment: Alignment.Vertical): Modifier = with(this@Row) {
                            align(alignment)
                        }

                        override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = with(this@Row) {
                            alignBy(alignmentLineBlock)
                        }

                        override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = with(this@Row) {
                            alignBy(alignmentLine)
                        }

                        override fun Modifier.alignByBaseline(): Modifier = with(this@Row) {
                            alignByBaseline()
                        }

                        override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = with(this@Row) {
                            weight(weight, fill)
                        }
                    }
                )
            }
        }
    }
}
