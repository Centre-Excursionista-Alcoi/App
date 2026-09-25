@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("CAST_NEVER_SUCCEEDS") // Kotlin/Native bridges String and NSString at runtime.

package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val KEYCHAIN_SERVICE = "org.centrexcursionistalcoi.app.credentials"

/**
 * Keeps Security's CF constants as native pointers. CFType callbacks give keys value equality and let
 * the dictionary retain/release its entries. Every Create/Retain is balanced within the query scope.
 */
private class KeychainQuery {
    val dictionary = checkNotNull(
        CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    )

    fun set(key: COpaquePointer?, value: COpaquePointer?) {
        CFDictionarySetValue(dictionary, key, value)
    }

    fun setObject(key: COpaquePointer?, value: Any) {
        val retained = checkNotNull(CFBridgingRetain(value))
        try {
            set(key, retained)
        } finally {
            CFRelease(retained)
        }
    }

    fun close() {
        CFRelease(dictionary)
    }
}

private inline fun <T> withKeychainQuery(block: KeychainQuery.() -> T): T {
    val query = KeychainQuery()
    return try {
        query.block()
    } finally {
        query.close()
    }
}

/** Converts a borrowed dictionary value without transferring the dictionary's ownership to ARC. */
private fun CFDictionaryRef.objectForKey(key: COpaquePointer?): Any? =
    CFDictionaryGetValue(this, key)?.let { CFBridgingRelease(CFRetain(it)) }

/**
 * Stores the session as one generic-password item (account = email, data = refresh token) in its own Keychain
 * service, only readable on this device and once it has been unlocked since booting (so background syncs can
 * refresh tokens). The password saved by versions before token authentication is another item, in the legacy
 * service, deleted once the session is saved.
 */
@Singleton
actual class CredentialsStore internal constructor() {
    private var service: String = KEYCHAIN_SERVICE

    internal constructor(service: String) : this() {
        this.service = service
    }

    private val sessionService: String get() = "$service.session"

    private val log = logging()

    private val mutableCurrent: MutableStateFlow<SavedAccount?> by lazy { MutableStateFlow(readAccount()) }
    actual val current: StateFlow<SavedAccount?> get() = mutableCurrent

    actual fun saveSession(email: String, refreshToken: String) {
        writeItem(sessionService, email, refreshToken)
        deleteItem(service)
        mutableCurrent.value = readAccount()
    }

    actual fun getSession(): SavedSession? =
        readItem(sessionService)?.let { (email, refreshToken) -> SavedSession(email, refreshToken) }

    actual fun getLegacyCredentials(): SavedCredentials? =
        readItem(service)?.let { (email, password) -> SavedCredentials(email, password.toCharArray()) }

    actual fun clear() {
        deleteItem(sessionService)
        deleteItem(service)
        mutableCurrent.value = readAccount()
    }

    /** Writes a password the way versions before token authentication did. Only for tests. */
    internal fun saveLegacyCredentialsForTests(email: String, password: String) {
        writeItem(service, email, password)
        mutableCurrent.value = readAccount()
    }

    private fun readAccount(): SavedAccount? =
        (readItem(sessionService) ?: readItem(service))?.let { (email) -> SavedAccount(email) }

    private fun writeItem(service: String, account: String, secret: String) {
        val secretData = (secret as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        withServiceQuery(service) {
            val query = dictionary
            withKeychainQuery {
                setObject(kSecAttrAccount, account)
                setObject(kSecValueData, secretData)
                set(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
                // Update the account and secret together, preserving the old item if the write fails.
                var status = SecItemUpdate(query, dictionary)
                if (status == errSecItemNotFound) {
                    set(kSecClass, kSecClassGenericPassword)
                    setObject(kSecAttrService, service)
                    status = SecItemAdd(dictionary, null)
                }
                if (status != errSecSuccess) log.w { "Keychain save failed (status=$status)." }
            }
        }
    }

    private fun deleteItem(service: String) {
        withServiceQuery(service) {
            val status = SecItemDelete(dictionary)
            if (status != errSecSuccess && status != errSecItemNotFound) {
                log.w { "Keychain delete failed (status=$status)." }
            }
        }
    }

    /** @return the item's account and secret. */
    private fun readItem(service: String): Pair<String, String>? = withServiceQuery(service) {
        set(kSecReturnAttributes, kCFBooleanTrue)
        set(kSecReturnData, kCFBooleanTrue)
        set(kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            result.value = null
            val status = SecItemCopyMatching(dictionary, result.ptr)
            try {
                if (status == errSecItemNotFound) return@memScoped null
                if (status != errSecSuccess) {
                    log.w { "Keychain read failed (status=$status)." }
                    return@memScoped null
                }
                // Request both fields in one query so they always belong to the same saved item.
                val item: CFDictionaryRef = result.value?.reinterpret() ?: return@memScoped null
                val account = item.objectForKey(kSecAttrAccount) as? String ?: return@memScoped null
                val data = item.objectForKey(kSecValueData) as? NSData ?: return@memScoped null
                val secret = NSString.create(data, NSUTF8StringEncoding) as? String ?: return@memScoped null
                account to secret
            } finally {
                result.value?.let { CFRelease(it) }
            }
        }
    }

    private inline fun <T> withServiceQuery(service: String, block: KeychainQuery.() -> T): T = withKeychainQuery {
        set(kSecClass, kSecClassGenericPassword)
        setObject(kSecAttrService, service)
        block()
    }
}
