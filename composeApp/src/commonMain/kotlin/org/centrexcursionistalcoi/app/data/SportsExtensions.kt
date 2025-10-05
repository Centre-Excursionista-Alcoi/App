package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

val Sports.displayName: String
    @Composable
    get() = when (this) {
        Sports.CLIMBING -> stringResource(Res.string.sport_climbing)
        Sports.CLIMBING_WITHOUT_BELAY -> stringResource(Res.string.sport_climbing_without_belay)
        Sports.VIA_FERRATA -> stringResource(Res.string.sport_via_ferrata)
        Sports.CANYONING -> stringResource(Res.string.sport_canyoning)
        Sports.HIKING -> stringResource(Res.string.sport_hiking)
        Sports.ALPINISM -> stringResource(Res.string.sport_alpinism)
    }
