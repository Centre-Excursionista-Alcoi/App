package backend

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.realtime.Realtime
import org.centrexcursionistalcoi.app.BuildKonfig

val supabase = createSupabaseClient(BuildKonfig.SUPABASE_URL, BuildKonfig.SUPABASE_KEY) {
    install(Auth)
    install(Realtime)
}
