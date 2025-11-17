# How to create migrations

See: https://sqldelight.github.io/sqldelight/2.0.2/multiplatform_sqlite/migrations/

1. Update the `.sq` table definition.
2. Add the required SQL logic to `migrations/<from-version.sqm>`.
3. Run the `generateCommonMainDatabaseSchema` Gradle task.
