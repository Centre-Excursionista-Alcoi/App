package backend.wrapper

import buildkonfig.BuildKonfig
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
    lateinit var postgrest: PostgrestWrapper
        private set

    lateinit var auth: Auth
        private set

    lateinit var storage: Storage
        private set

    fun initialize(client: SupabaseClient) {
        auth = client.auth
        postgrest = PostgrestWrapper(client.postgrest)
        storage = client.storage
    }

    init {
        initialize(supabase)
    }
}
