package org.centrexcursionistalcoi.app.utils

import javax.money.Monetary
import javax.money.MonetaryAmount
import org.centrexcursionistalcoi.app.server.response.data.MoneyD

/**
 * Convert a [MonetaryAmount] to a [MoneyD].
 */
fun MoneyD.Companion.fromMonetaryAmount(amount: MonetaryAmount): MoneyD {
    return MoneyD(
        amount.currency.currencyCode,
        amount.number.doubleValueExact(),
        amount.factory::class.java.name
    )
}

fun MonetaryAmount.serializable(): MoneyD = MoneyD.fromMonetaryAmount(this)

/**
 * Convert a [MoneyD] to a [MonetaryAmount].
 */
fun MoneyD.toMonetaryAmount(): MonetaryAmount {
    val factories = Monetary.getAmountFactories()
    require(factories.isNotEmpty()) { "No factories available" }
    val factory = factories.find { it::class.java.name == factory } ?: factories.first()

    factory.setCurrency(currency)
    factory.setNumber(amount)

    return factory.create()
}
