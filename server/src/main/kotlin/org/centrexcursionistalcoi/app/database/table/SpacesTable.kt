package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.money.compositeMoney
import org.jetbrains.exposed.sql.money.currency

object SpacesTable : IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val name = varchar("name", 255)
    val description = text("description").nullable()

    // General characteristics of the space
    val capacity = uinteger("capacity").nullable()

    // Pricing information for members
    private val memberPriceAmount = decimal("member_price_amount", 10, 2).nullable()
    private val memberPriceCurrency = currency("member_price_currency").nullable()
    val memberPrice = compositeMoney(memberPriceAmount, memberPriceCurrency)

    // Pricing information for non-members
    private val externalPriceAmount = decimal("non_member_price_amount", 10, 2).nullable()
    private val externalPriceCurrency = currency("non_member_price_currency").nullable()
    val externalPrice = compositeMoney(externalPriceAmount, externalPriceCurrency)

    // Coordinates of the space
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()

    // Address of the space
    val address = varchar("address", 255).nullable()
    val city = varchar("city", 255).nullable()
    val postalCode = varchar("postal_code", 255).nullable()
    val country = varchar("country", 255).nullable()
}
