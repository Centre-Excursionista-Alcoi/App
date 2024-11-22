package org.centrexcursionistalcoi.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toPath

fun createDataStore(context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
    File(context.dataDir, "settings.preferences_pb").absolutePath.toPath()
}
