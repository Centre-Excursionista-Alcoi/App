package org.centrexcursionistalcoi.app.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

fun <T: Any, V> setPrivateValue(instance: T, kClass: KClass<T>, name: String, value: V) {
    val prop = kClass.memberProperties.find { it.name == name }
    requireNotNull(prop) { "Could not find property named \"$name\" in ${kClass.simpleName}.\nProperties: ${kClass.memberProperties.joinToString { it.name }}" }

    prop.isAccessible = true

    val javaField = prop.javaField!!
    println("Setting javaField (${javaField.name}) of ${kClass.simpleName} to $value (${value?.let { it::class.simpleName }})")
    javaField.isAccessible = true
    javaField.set(instance, value)
}

fun <T: Any, V> setPrivateDelegatedValue(instance: T, kClass: KClass<T>, name: String, value: V) {
    val delegateFieldName = "$name\$delegate"

    // 1. Walk class hierarchy to find the field
    var clazz: Class<*>? = kClass.java
    var delegateField: java.lang.reflect.Field? = null
    while (clazz != null && delegateField == null) {
        try {
            delegateField = clazz.getDeclaredField(delegateFieldName)
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass // climb up
        }
    }

    requireNotNull(delegateField) {
        "No delegate field found for property '$name'"
    }

    delegateField.isAccessible = true

    // 2. Extract the delegate object
    val delegate = delegateField.get(instance) as ReadWriteProperty<Any?, V>

    // 3. Find the KProperty for the delegated variable
    val kProp = instance::class.memberProperties.first { it.name == name } as KProperty<V>

    // 4. Call setValue on the delegate
    delegate.setValue(instance, kProp, value)
}
