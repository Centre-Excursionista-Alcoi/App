package backend.users

import backend.data.user.Role
import backend.supabase
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest

suspend fun UserInfo.getRoles(): List<Role> {
    return supabase.postgrest
        .from("user_roles")
        .select {
            filter { eq("user_id", id) }
        }
        .decodeList<Role>()
}
