package backend.wrapper

import buildkonfig.BuildKonfig
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

private val supabase = createSupabaseClient(BuildKonfig.SUPABASE_URL, BuildKonfig.SUPABASE_KEY) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}

object SupabaseWrapper {
    lateinit var postgrest: IPostgrestWrapper
        private set

    lateinit var auth: Auth
        private set

    lateinit var storage: Storage
        private set

    private inline fun initialize(module: String, set: () -> Unit) {
        try {
            set()
        } catch (_: IllegalStateException) {
            Napier.w(tag = "SupabaseWrapper") {
                "Could not initialize Supabase $module: Not installed"
            }
        }
    }

    fun initialize(client: SupabaseClient) {
        initialize("Auth") {
            auth = client.auth
        }
        initialize("Postgrest") {
            postgrest = PostgrestWrapper(client.postgrest)
        }
        initialize("Storage") {
            storage = client.storage
        }
    }

    /**
     * Reset the SupabaseWrapper to use the default SupabaseClient.
     */
    fun reset() {
        initialize(supabase)
    }

    init {
        // Initialize the default SupabaseClient if not in testing mode
        // Tests will initialize the SupabaseClient by themselves
        if (!BuildKonfig.TESTING) reset()
    }
}
