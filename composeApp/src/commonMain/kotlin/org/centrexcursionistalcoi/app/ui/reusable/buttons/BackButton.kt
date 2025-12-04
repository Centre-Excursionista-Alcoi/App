package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.back
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick
    ) {
        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
    }
}
