package org.centrexcursionistalcoi.app.permission

import tech.kotlinlang.permission.HelperHolder as LibHelperHolder

actual object HelperHolder {
    actual fun getPermissionHelperInstance(): PermissionHelper = LibHelperHolder.getPermissionHelperInstance().asAppPermissionHelper()
}
