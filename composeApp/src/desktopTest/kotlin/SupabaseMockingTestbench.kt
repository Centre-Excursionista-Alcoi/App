import backend.wrapper.IPostgrestWrapper
import backend.wrapper.SupabaseWrapper
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.mockk.every
import io.mockk.mockkObject
import kotlin.reflect.KClass
import kotlin.test.BeforeTest

abstract class SupabaseMockingTestbench(
    supabaseWrapperConfig: (SupabaseClientBuilder.() -> Unit)? = null
) : MockingTestbench(supabaseWrapperConfig) {
    protected var selectList: (
        table: String,
        kClass: KClass<*>,
        columns: Columns,
        head: Boolean,
        request: PostgrestRequestBuilder.() -> Unit
    ) -> List<*> = { _, _, _, _, _ -> emptyList<Any>() }

    @Suppress("UNCHECKED_CAST")
    private val mockPostgrestWrapper = object : IPostgrestWrapper {
        override suspend fun <Type : Any> selectList(
            table: String,
            kClass: KClass<Type>,
            columns: Columns,
            head: Boolean,
            request: PostgrestRequestBuilder.() -> Unit
        ): List<Type> = this@SupabaseMockingTestbench.selectList(table, kClass, columns, head, request) as List<Type>

        override suspend fun <Type : Any> selectOrNull(
            table: String,
            kClass: KClass<Type>,
            columns: Columns,
            head: Boolean,
            request: PostgrestRequestBuilder.() -> Unit
        ): Type? {
            TODO("Not yet implemented")
        }

        override suspend fun <Type : Any> insert(
            table: String,
            items: List<Type>
        ): PostgrestResult {
            TODO("Not yet implemented")
        }

        override suspend fun <Type : Any> insert(table: String, element: Type): PostgrestResult {
            TODO("Not yet implemented")
        }

        override suspend fun <Type : Any> update(
            table: String,
            element: Type,
            request: PostgrestRequestBuilder.() -> Unit
        ): PostgrestResult {
            TODO("Not yet implemented")
        }

        override suspend fun update(
            table: String,
            update: PostgrestUpdate.() -> Unit,
            request: PostgrestRequestBuilder.() -> Unit
        ): PostgrestResult {
            TODO("Not yet implemented")
        }
    }

    @BeforeTest
    fun `mock postgrest wrapper`() {
        mockkObject(SupabaseWrapper)
        every { SupabaseWrapper.postgrest } returns mockPostgrestWrapper
    }
}
