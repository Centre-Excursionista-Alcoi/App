package org.centrexcursionistalcoi.app.di

/**
 * Starts Koin, for Swift to call from `AppDelegate.application(_:didFinishLaunchingWithOptions:)`.
 *
 * [initKoin] itself can't be called directly from Swift: its signature exposes `Module`/`KoinApplication`, types
 * owned by the Koin library rather than this module, and Kotlin/Native's Swift/ObjC export only exposes
 * declarations whose signature is made up of types from the module being compiled -- a public function referencing
 * an un-exported dependency's type is silently dropped from the generated header. This wrapper takes no arguments,
 * so it has nothing to drop.
 */
fun initKoinIos() {
    initKoin()
}
