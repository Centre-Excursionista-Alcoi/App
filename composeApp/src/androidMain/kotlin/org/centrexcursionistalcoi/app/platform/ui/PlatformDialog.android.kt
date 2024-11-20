package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    title: String?,
    actions: @Composable PlatformDialogContext.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                content()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
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
}
