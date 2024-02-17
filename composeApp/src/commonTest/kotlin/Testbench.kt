import backend.wrapper.SupabaseWrapper
import buildkonfig.BuildKonfig.SUPABASE_KEY
import buildkonfig.BuildKonfig.SUPABASE_URL
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.createSupabaseClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class Testbench(
    private val supabaseWrapperConfig: (SupabaseClientBuilder.() -> Unit)? = null
) {
    /**
     * This flag is used to determine if the [SupabaseWrapper] has been prepared.
     * It is used to reset the [SupabaseWrapper] after the test has been executed.
     */
    private var hasPreparedSupabaseWrapper = false

    @BeforeTest
    fun setup() {
        if (supabaseWrapperConfig != null) {
            // calls will be mocked, so no need to add urls
            val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY, supabaseWrapperConfig)
            SupabaseWrapper.initialize(client)
            hasPreparedSupabaseWrapper = true
        }
    }

    @AfterTest
    fun cleanup() {
        if (hasPreparedSupabaseWrapper) {
            SupabaseWrapper.reset()
        }
    }
}
