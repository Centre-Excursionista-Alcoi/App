package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object SpaceLendingTransactions : Table("space_lending_transactions") {
    val lending = reference("lending", SpaceLendings, onDelete = ReferenceOption.CASCADE)
    val transaction = reference("transaction", Transactions, onDelete = ReferenceOption.CASCADE)

    override val primaryKey: PrimaryKey = PrimaryKey(lending, transaction, name = "PK_SpaceLendingTransactions_lending_transaction")
}
