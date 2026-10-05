package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.data.SpaceLending
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
