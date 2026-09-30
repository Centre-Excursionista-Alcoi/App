package org.centrexcursionistalcoi.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

internal const val dataStoreFileName = "cea.preferences_pb"

/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
fun createDataStore(storage: Storage<Preferences>): DataStore<Preferences> = DataStoreFactory.create(storage = storage)

expect fun createDataStore(): DataStore<Preferences>
