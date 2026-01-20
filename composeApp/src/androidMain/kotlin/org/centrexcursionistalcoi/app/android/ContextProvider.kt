package org.centrexcursionistalcoi.app.android

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

class ContextProvider : ContentProvider() {
    companion object {
        var context: Context? = null
            private set
    }

    override fun onCreate(): Boolean {
        val context = context ?: return false
        ContextProvider.context = context
        return true
    }

    override fun shutdown() {
        super.shutdown()
        ContextProvider.context = null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String?>?): Int = 0
}