package org.centrexcursionistalcoi.app.storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable

@OptIn(ExperimentalSettingsApi::class)
val settings: ObservableSettings = Settings().makeObservable()

/**
 * Key for storing the last profile synchronization timestamp.
 */
const val SETTINGS_LAST_PROFILE_SYNC = "last_profile_sync"

const val SETTINGS_LAST_DEPARTMENTS_SYNC = "last_departments_sync"
const val SETTINGS_LAST_INVENTORY_ITEMS_SYNC = "last_inventory_items_sync"
const val SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC = "last_inventory_item_types_sync"
const val SETTINGS_LAST_LENDINGS_SYNC = "last_lendings_sync"
const val SETTINGS_LAST_POSTS_SYNC = "last_posts_sync"
const val SETTINGS_LAST_USERS_SYNC = "last_users_sync"
const val SETTINGS_LAST_EVENTS_SYNC = "last_events_sync"
const val SETTINGS_LAST_MEMBERS_SYNC = "last_members_sync"

/**
 * Key for storing the selected language in the settings.
 */
const val SETTINGS_LANGUAGE = "language"

const val SETTINGS_PRIVACY_ERRORS = "report_errors"
const val SETTINGS_PRIVACY_ANALYTICS = "share_analytics"
const val SETTINGS_PRIVACY_SESSION_REPLAY = "session_replay"

const val MANAGEMENT_TOGGLE_COMPLETED_LENDINGS = "management_toggle_completed_lendings"

/** Stores the server info in cache for situations where Internet is not available. */
const val SETTINGS_SERVER_INFO = "server_info"
