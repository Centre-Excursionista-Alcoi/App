package backend.wrapper

import io.github.jan.supabase.gotrue.PostgrestFilterDSL
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.DeserializationStrategy

interface IPostgrestWrapper {
    /**
     * Runs a select statement for extracting a list of items from the database.
     */
    suspend fun <Type: Any> selectList(
        table: String,
        serializer: DeserializationStrategy<Type>,
        columns: Columns = Columns.ALL,
        head: Boolean = false,
        request: @PostgrestFilterDSL (PostgrestRequestBuilder.() -> Unit) = {}
    ): List<Type>

    /**
     * Runs a select statement for extracting a single item from the database.
     */
    suspend fun <Type: Any> selectOrNull(
        table: String,
        serializer: DeserializationStrategy<Type>,
        columns: Columns = Columns.ALL,
        head: Boolean = false,
        request: @PostgrestFilterDSL (PostgrestRequestBuilder.() -> Unit) = {}
    ): Type?

    /**
     * Runs an insert statement for adding a list of items to the database.
     */
    suspend fun <Type: Any> insert(table: String, items: List<Type>): PostgrestResult

    /**
     * Runs an insert statement for adding an item to the database.
     */
    suspend fun <Type: Any> insert(table: String, element: Type): PostgrestResult

    /**
     * Runs an update statement for updating an element in the database.
     */
    suspend fun <Type: Any> update(
        table: String,
        element: Type,
        request: PostgrestRequestBuilder.() -> Unit = {}
    ): PostgrestResult

    /**
     * Runs an update statement for updating an element in the database.
     */
    suspend fun update(
        table: String,
        update: PostgrestUpdate.() -> Unit = {},
        request: PostgrestRequestBuilder.() -> Unit = {}
    ): PostgrestResult
}
