package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloseButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(MaterialSymbols.Close, stringResource(Res.string.close))
    }
}
