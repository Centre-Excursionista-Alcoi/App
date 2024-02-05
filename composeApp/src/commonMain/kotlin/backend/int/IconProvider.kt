package backend.int

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.NordicWalking
import androidx.compose.material.icons.outlined.Snowshoeing
import androidx.compose.ui.graphics.vector.ImageVector
import ui.icons.ExtIcons
import ui.icons.exticons.Carabiner

interface IconProvider {
    val icon: String?

    companion object {
        val icons = mapOf(
            "hiking" to Icons.Outlined.Hiking,
            "nordic_walking" to Icons.Outlined.NordicWalking,
            "snowshoeing" to Icons.Outlined.Snowshoeing,
            "carabiner" to ExtIcons.Carabiner
        )
    }
}

val IconProvider?.imageVector: ImageVector
    get() = this?.icon?.let(IconProvider.icons::get) ?: Icons.Outlined.Category
