package backend.users

import backend.data.user.Role
import backend.wrapper.SupabaseWrapper
import io.github.jan.supabase.gotrue.user.UserInfo

suspend fun UserInfo.getRoles(): List<Role> =
    SupabaseWrapper.postgrest
        .selectList("user_roles", Role::class) {
            filter { eq("user_id", id) }
        }
