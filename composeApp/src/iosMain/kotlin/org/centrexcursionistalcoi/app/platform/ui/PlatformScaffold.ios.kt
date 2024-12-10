package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoNavigateBackButton
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformScaffold(
    title: String?,
    actions: List<Action>,
    navigationBar: @Composable (() -> Unit)?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
) {
    CupertinoScaffold(
        topBar = {
            if (title != null) {
                CupertinoTopAppBar(
                    title = { CupertinoText(title) },
                    actions = {
                        for (action in actions) {
                            CupertinoIconButton(
                                enabled = action.enabled,
                                onClick = action.onClick
                            ) {
                                CupertinoIcon(action.icon, action.label)
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            CupertinoNavigateBackButton(
                                title = { CupertinoText(stringResource(Res.string.back)) },
                                onClick = onBack
                            )
                        }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it)
        ) {
            content(PaddingValues(0.dp))
        }
    }
}
