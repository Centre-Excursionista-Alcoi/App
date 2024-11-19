package org.centrexcursionistalcoi.app.utils

import javax.money.Monetary
import javax.money.MonetaryAmount
import org.centrexcursionistalcoi.app.data.MoneyD

/**
 * Convert a [MonetaryAmount] to a [MoneyD].
 */
fun MoneyD.Companion.fromMonetaryAmount(amount: MonetaryAmount): MoneyD {
    return MoneyD(
        amount.currency.currencyCode,
        amount.number.doubleValueExact()
    )
}

fun MonetaryAmount.serializable(): MoneyD = MoneyD.fromMonetaryAmount(this)

/**
 * Convert a [MoneyD] to a [MonetaryAmount].
 */
fun MoneyD.toMonetaryAmount(): MonetaryAmount {
    return Monetary.getDefaultAmountFactory()
        .setCurrency(currency)
        .setNumber(amount)
        .create()
}
