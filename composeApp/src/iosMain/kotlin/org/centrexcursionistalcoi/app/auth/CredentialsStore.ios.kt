package org.centrexcursionistalcoi.app.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import org.koin.core.annotation.Singleton

private const val KEYCHAIN_SERVICE = "org.centrexcursionistalcoi.app.credentials"

/**
 * iOS equivalent of Android's `AccountManager`-backed [CredentialsStore] (see the `androidMain` actual):
 * persists the last successful login's credentials in the system Keychain, as a single
 * `kSecClassGenericPassword` item scoped to [KEYCHAIN_SERVICE], so [AuthBackend.tryAutoRelogin] can silently
 * re-authenticate here too instead of always falling through to a normal logout.
 */
@OptIn(ExperimentalForeignApi::class)
@Singleton
actual class CredentialsStore {
    // Unlike Android's AccountManager, nothing outside this app's own process can write to this Keychain item,
    // so there's no OS-level listener needed here -- save()/clear() just update this directly.
    actual val current: StateFlow<SavedCredentials?>
        field = MutableStateFlow(readCurrent())

    actual fun save(email: String, password: String) {
        // Only one saved account at a time, mirroring the Android actual.
        clear()

        val passwordData = (password as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val attributes = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass as NSString)
            setObject(KEYCHAIN_SERVICE, forKey = kSecAttrService as NSString)
            setObject(email, forKey = kSecAttrAccount as NSString)
            setObject(passwordData, forKey = kSecValueData as NSString)
        }
        SecItemAdd(attributes.asCFDictionary(), null)
        current.value = readCurrent()
    }

    actual fun get(): SavedCredentials? = readCurrent()

    actual fun clear() {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass as NSString)
            setObject(KEYCHAIN_SERVICE, forKey = kSecAttrService as NSString)
        }
        SecItemDelete(query.asCFDictionary())
        current.value = readCurrent()
    }

    private fun readCurrent(): SavedCredentials? {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass as NSString)
            setObject(KEYCHAIN_SERVICE, forKey = kSecAttrService as NSString)
            setObject(true, forKey = kSecReturnAttributes as NSString)
            setObject(true, forKey = kSecReturnData as NSString)
            setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as NSString)
        }

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query.asCFDictionary(), result.ptr)
            if (status != errSecSuccess) return@memScoped null

            @Suppress("UNCHECKED_CAST")
            val item = CFBridgingRelease(result.value) as? Map<Any?, Any?> ?: return@memScoped null
            val email = item[kSecAttrAccount as String] as? String ?: return@memScoped null
            val data = item[kSecValueData as String] as? NSData ?: return@memScoped null
            val password = NSString.create(data, NSUTF8StringEncoding) as? String ?: return@memScoped null
            SavedCredentials(email, password.toCharArray())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSMutableDictionary.asCFDictionary(): CFDictionaryRef? = CFBridgingRetain(this) as CFDictionaryRef?
