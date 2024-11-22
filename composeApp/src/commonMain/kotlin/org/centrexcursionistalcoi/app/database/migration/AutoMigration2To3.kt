package org.centrexcursionistalcoi.app.database.migration

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "Space",
    columnName = "membercurrency"
)
@RenameColumn(
    tableName = "Space",
    fromColumnName = "memberamount",
    toColumnName = "memberPrice"
)
@DeleteColumn(
    tableName = "Space",
    columnName = "externalcurrency"
)
@RenameColumn(
    tableName = "Space",
    fromColumnName = "externalamount",
    toColumnName = "externalPrice"
)
class AutoMigration2To3: AutoMigrationSpec
