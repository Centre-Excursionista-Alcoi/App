package backend.wrapper

import io.github.jan.supabase.gotrue.PostgrestFilterDSL
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlin.reflect.KClass

class PostgrestWrapper(private val postgrest: Postgrest) : IPostgrestWrapper {
    /**
     * Runs a select statement for extracting a list of items from the database.
     */
    override suspend fun <Type : Any> selectList(
        table: String,
        kClass: KClass<Type>,
        columns: Columns,
        head: Boolean,
        request: @PostgrestFilterDSL (PostgrestRequestBuilder.() -> Unit)
    ): List<Type> {
        return postgrest.from(table)
            .select(columns, head, request)
            .decodeList<Any>()
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                if (it::class == kClass)
                    it as Type
                else
                    null
            }
    }

    /**
     * Runs a select statement for extracting a single item from the database.
     */
    override suspend fun <Type : Any> selectOrNull(
        table: String,
        kClass: KClass<Type>,
        columns: Columns,
        head: Boolean,
        request: @PostgrestFilterDSL (PostgrestRequestBuilder.() -> Unit)
    ): Type? {
        val decoded = postgrest.from(table)
            .select(columns, head, request)
            .decodeSingleOrNull<Any>()
        @Suppress("UNCHECKED_CAST")
        (return if (decoded != null && decoded::class == kClass)
            decoded as Type
        else
            null)
    }

    /**
     * Runs an insert statement for adding a list of items to the database.
     */
    override suspend fun <Type : Any> insert(table: String, items: List<Type>): PostgrestResult {
        // Convert to List<Any> so that inlining is not required
        val list: List<Any> = items
        return postgrest.from(table).insert(list)
    }

    /**
     * Runs an insert statement for adding an item to the database.
     */
    override suspend fun <Type : Any> insert(table: String, element: Type): PostgrestResult {
        return postgrest.from(table).insert(element as Any)
    }

    /**
     * Runs an update statement for updating an element in the database.
     */
    override suspend fun <Type : Any> update(
        table: String,
        element: Type,
        request: PostgrestRequestBuilder.() -> Unit
    ): PostgrestResult {
        return postgrest.from(table).update(element as Any, request)
    }

    /**
     * Runs an update statement for updating an element in the database.
     */
    override suspend fun update(
        table: String,
        update: PostgrestUpdate.() -> Unit,
        request: PostgrestRequestBuilder.() -> Unit
    ): PostgrestResult {
        return postgrest.from(table).update(update, request)
    }
}
