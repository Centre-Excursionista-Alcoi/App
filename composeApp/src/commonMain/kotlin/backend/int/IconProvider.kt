package backend.int

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.NordicWalking
import androidx.compose.material.icons.outlined.Snowshoeing
import androidx.compose.ui.graphics.vector.ImageVector
import ui.icons.ExtIcons
import ui.icons.exticons.Carabiner
import ui.icons.exticons.Climbing
import ui.icons.exticons.ClimbingHelmet
import ui.icons.exticons.ClimbingShoes
import ui.icons.exticons.EnergyAbsorber
import ui.icons.exticons.Quickdraw
import ui.icons.exticons.Rope

interface IconProvider {
    val icon: String?

    companion object {
        val icons = mapOf(
            "hiking" to Icons.Outlined.Hiking,
            "nordic_walking" to Icons.Outlined.NordicWalking,
            "snowshoeing" to Icons.Outlined.Snowshoeing,
            "carabiner" to ExtIcons.Carabiner,
            "climbing" to ExtIcons.Climbing,
            "climbing_helmet" to ExtIcons.ClimbingHelmet,
            "climbing_shoes" to ExtIcons.ClimbingShoes,
            "energy_absorber" to ExtIcons.EnergyAbsorber,
            "quickdraw" to ExtIcons.Quickdraw,
            "rope" to ExtIcons.Rope
        )
    }
}

val IconProvider?.imageVector: ImageVector
    get() = this?.icon?.let(IconProvider.icons::get) ?: Icons.Outlined.Category
