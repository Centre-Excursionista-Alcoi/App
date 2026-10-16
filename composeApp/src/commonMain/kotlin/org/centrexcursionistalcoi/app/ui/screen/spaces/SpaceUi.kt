package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ClimateMiniSplit
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Fireplace
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Hvac
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ModeFan
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.SolarPower
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.WindPower
import org.centrexcursionistalcoi.app.ui.icons.mdi.FuelCell
import org.centrexcursionistalcoi.app.ui.icons.mdi.GeneratorStationary
import org.centrexcursionistalcoi.app.ui.icons.mdi.HydroPower
import org.centrexcursionistalcoi.app.ui.icons.mdi.MDI
import org.centrexcursionistalcoi.app.ui.icons.mdi.TransmissionTower
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
fun Category.label(): String = stringResource(
    when (this) {
        Category.MEMBER -> Res.string.category_member
        Category.NON_MEMBER -> Res.string.category_non_member
        Category.CHILD_MEMBER -> Res.string.category_child_member
        Category.CHILD_NON_MEMBER -> Res.string.category_child_non_member
    }
)

@Composable
fun PriceUnit.label(): String = stringResource(
    when (this) {
        PriceUnit.PER_NIGHT -> Res.string.price_unit_night
        PriceUnit.PER_DAY -> Res.string.price_unit_day
    }
)

@Composable
fun PaymentStatus.label(): String = stringResource(
    when (this) {
        PaymentStatus.PENDING -> Res.string.payment_status_pending
        PaymentStatus.COMPLETED -> Res.string.payment_status_completed
        PaymentStatus.FAILED -> Res.string.payment_status_failed
        PaymentStatus.REFUNDED -> Res.string.payment_status_refunded
        PaymentStatus.CANCELLED -> Res.string.payment_status_cancelled
    }
)

/** The steps of a space lending. */
enum class SpaceLendingStage {
    CANCELLED,
    /** Booked: it can still be changed. */
    CREATED,
    /** The keys were handed over: it is locked. */
    PICKED_UP,
    /** The keys are back, the payment is pending. */
    RETURNED,
    PAID,
}

val SpaceLending.stage: SpaceLendingStage
    get() = when {
        cancelled -> SpaceLendingStage.CANCELLED
        paymentStatus == PaymentStatus.COMPLETED || paymentStatus == PaymentStatus.REFUNDED -> SpaceLendingStage.PAID
        returnedAt != null -> SpaceLendingStage.RETURNED
        pickedUpAt != null -> SpaceLendingStage.PICKED_UP
        else -> SpaceLendingStage.CREATED
    }

@Composable
fun SpaceLendingStage.label(): String = stringResource(
    when (this) {
        SpaceLendingStage.CANCELLED -> Res.string.space_lending_stage_cancelled
        SpaceLendingStage.CREATED -> Res.string.space_lending_stage_created
        SpaceLendingStage.PICKED_UP -> Res.string.space_lending_stage_picked_up
        SpaceLendingStage.RETURNED -> Res.string.space_lending_stage_returned
        SpaceLendingStage.PAID -> Res.string.space_lending_stage_paid
    }
)

/** Formats [price] as euros, with two decimals. */
fun formatPrice(price: Double): String {
    val cents = (price * 100).roundToLong()
    return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')} €"
}

fun LocalDate.formatted(): String = toString()

@Composable
fun SpaceEnergySource.label(): String = stringResource(
    when (this) {
        SpaceEnergySource.GRID -> Res.string.energy_grid
        SpaceEnergySource.SOLAR -> Res.string.energy_solar
        SpaceEnergySource.WIND -> Res.string.energy_wind
        SpaceEnergySource.HYDRO -> Res.string.energy_hydro
        SpaceEnergySource.DIESEL_GENERATOR -> Res.string.energy_diesel_generator
        SpaceEnergySource.GAS_GENERATOR -> Res.string.energy_gas_generator
        SpaceEnergySource.BATTERIES -> Res.string.energy_batteries
    }
)

fun SpaceEnergySource.icon(): ImageVector = when(this) {
    SpaceEnergySource.GRID -> MDI.TransmissionTower
    SpaceEnergySource.SOLAR -> MaterialSymbols.SolarPower
    SpaceEnergySource.WIND -> MaterialSymbols.WindPower
    SpaceEnergySource.HYDRO -> MDI.HydroPower
    SpaceEnergySource.GAS_GENERATOR, SpaceEnergySource.DIESEL_GENERATOR -> MDI.GeneratorStationary
    SpaceEnergySource.BATTERIES -> MDI.FuelCell
}

@Composable
fun SpaceClimateControl.label(): String = stringResource(
    when (this) {
        SpaceClimateControl.GAS_STOVE -> Res.string.climate_gas_stove
        SpaceClimateControl.WOOD_STOVE -> Res.string.climate_wood_stove
        SpaceClimateControl.PELLET_STOVE -> Res.string.climate_pellet_stove
        SpaceClimateControl.FIREPLACE -> Res.string.climate_fireplace
        SpaceClimateControl.ELECTRIC_HEATER -> Res.string.climate_electric_heater
        SpaceClimateControl.CENTRAL_HEATING -> Res.string.climate_central_heating
        SpaceClimateControl.AIR_CONDITIONING -> Res.string.climate_air_conditioning
        SpaceClimateControl.FAN -> Res.string.climate_fan
    }
)

fun SpaceClimateControl.icon(): ImageVector = when (this) {
    SpaceClimateControl.GAS_STOVE -> MaterialSymbols.Fireplace
    SpaceClimateControl.WOOD_STOVE -> MaterialSymbols.Fireplace
    SpaceClimateControl.PELLET_STOVE -> MaterialSymbols.Fireplace
    SpaceClimateControl.FIREPLACE -> MaterialSymbols.Fireplace
    SpaceClimateControl.ELECTRIC_HEATER -> MaterialSymbols.Hvac
    SpaceClimateControl.CENTRAL_HEATING -> MaterialSymbols.Hvac
    SpaceClimateControl.AIR_CONDITIONING -> MaterialSymbols.ClimateMiniSplit
    SpaceClimateControl.FAN -> MaterialSymbols.ModeFan
}
