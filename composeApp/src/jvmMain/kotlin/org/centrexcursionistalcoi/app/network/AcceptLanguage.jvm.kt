package org.centrexcursionistalcoi.app.network

import java.util.Locale

actual fun systemLanguageTag(): String? = Locale.getDefault().toLanguageTag()
