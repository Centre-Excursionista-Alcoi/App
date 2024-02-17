import io.github.jan.supabase.SupabaseClientBuilder
import io.mockk.unmockkAll
import kotlin.test.AfterTest

abstract class MockingTestbench(
    supabaseWrapperConfig: (SupabaseClientBuilder.() -> Unit)? = null
): Testbench(supabaseWrapperConfig) {
    @AfterTest
    fun unmock() {
        unmockkAll()
    }
}
