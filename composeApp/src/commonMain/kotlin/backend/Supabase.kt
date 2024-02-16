package backend

import buildkonfig.BuildKonfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(BuildKonfig.SUPABASE_URL, BuildKonfig.SUPABASE_KEY) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
