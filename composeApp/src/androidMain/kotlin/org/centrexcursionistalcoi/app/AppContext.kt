package org.centrexcursionistalcoi.app

import android.content.Context

/**
 * Gets a valid application context from various sources.
 *
 * Priority is given to [MainActivity]'s context, then [ContextProvider]'s context, and finally [AppBase]'s instance context.
 *
 * @throws IllegalStateException if no valid context is found.
 */
val appContext: Context
    get() = MainActivity.instance?.applicationContext ?: ContextProvider.context ?: AppBase.instance ?: error("Could not find any valid context")
