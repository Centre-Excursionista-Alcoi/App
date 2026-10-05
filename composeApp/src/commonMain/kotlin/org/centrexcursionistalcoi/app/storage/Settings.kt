package org.centrexcursionistalcoi.app.storage

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Key for storing the last profile synchronization timestamp.
 */
val SETTINGS_LAST_PROFILE_SYNC = longPreferencesKey("last_profile_sync")

val SETTINGS_LAST_DEPARTMENTS_SYNC = longPreferencesKey("last_departments_sync")
val SETTINGS_LAST_INVENTORY_ITEMS_SYNC = longPreferencesKey("last_inventory_items_sync")
val SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC = longPreferencesKey("last_inventory_item_types_sync")
val SETTINGS_LAST_LENDINGS_SYNC = longPreferencesKey("last_lendings_sync")
val SETTINGS_LAST_POSTS_SYNC = longPreferencesKey("last_posts_sync")
val SETTINGS_LAST_USERS_SYNC = longPreferencesKey("last_users_sync")
val SETTINGS_LAST_EVENTS_SYNC = longPreferencesKey("last_events_sync")
val SETTINGS_LAST_MEMBERS_SYNC = longPreferencesKey("last_members_sync")
val SETTINGS_LAST_MEMORIES_SYNC = longPreferencesKey("last_memories_sync")
val SETTINGS_LAST_SPACES_SYNC = longPreferencesKey("last_spaces_sync")
val SETTINGS_LAST_SPACE_KEY_TYPES_SYNC = longPreferencesKey("last_space_key_types_sync")
val SETTINGS_LAST_SPACE_KEYS_SYNC = longPreferencesKey("last_space_keys_sync")
val SETTINGS_LAST_SPACE_LENDINGS_SYNC = longPreferencesKey("last_space_lendings_sync")

/**
 * Key for storing the selected language in the settings.
 */
val SETTINGS_LANGUAGE = stringPreferencesKey("language")

val SETTINGS_PRIVACY_ERRORS = booleanPreferencesKey("report_errors")
val SETTINGS_PRIVACY_ANALYTICS = booleanPreferencesKey("share_analytics")
val SETTINGS_PRIVACY_SESSION_REPLAY = booleanPreferencesKey("session_replay")

val MANAGEMENT_TOGGLE_COMPLETED_LENDINGS = booleanPreferencesKey("management_toggle_completed_lendings")

/**
 * Stores the server info in cache for situations where Internet is not available.
 */
// TODO: Move this to a more appropriate place other than settings.
val SETTINGS_SERVER_INFO = stringPreferencesKey("server_info")
