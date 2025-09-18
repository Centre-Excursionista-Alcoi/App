package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.cookies.CookiesStorage

class ApplicationTestContext<DIB>(
    val dibResult: DIB?,
    val cookiesStorage: CookiesStorage
)
