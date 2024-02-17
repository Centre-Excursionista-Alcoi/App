package backend

import SupabaseMockingTestbench
import backend.wrapper.SupabaseWrapper
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPostgrestWrapper : SupabaseMockingTestbench(
    supabaseWrapperConfig = {
        install(Postgrest)
    }
) {
    @Test
    fun `test select`() {
        val list = listOf("a", "b", "c")
        selectList = { _, _, _, _, _ -> list }
        val result = runBlocking {
            SupabaseWrapper.postgrest.selectList("table", String::class)
        }
        assertEquals(list, result)
    }
}
