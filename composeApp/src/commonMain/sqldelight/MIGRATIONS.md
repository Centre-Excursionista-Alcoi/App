# How to create migrations

1. Update the `.sq` table definition.
2. Add the required SQL logic to `migrations/<version.sqm>`.
3. Run the `generateCommonMainDatabaseSchema` Gradle task.
