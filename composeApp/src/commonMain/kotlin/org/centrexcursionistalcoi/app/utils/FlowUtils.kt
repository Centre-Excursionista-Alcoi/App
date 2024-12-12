package org.centrexcursionistalcoi.app.utils

inline fun <T> T.applyIf(predicate: () -> Boolean, block: T.() -> Unit): T = if (predicate()) this.apply(block) else this

inline fun <T, V> T.applyIfNotNull(value: V?, block: T.(V) -> Unit): T =
    if (value != null)
        this.apply { block(value) }
    else
        this

/**
 * Executes the given block of code only if all provided values are not null, based on the specified condition.
 *
 * @param values A variable number of nullable values to be checked.
 * @param condition A lambda that defines how the results of the null-checks should be combined (default is logical AND).
 * @param block A lambda to be executed on the instance if the condition is satisfied.
 * @return The caller instance, whether the block was executed or not.
 */
fun <T, V> T.applyIfNotNull(
    vararg values: V?,
    condition: Boolean.(Boolean) -> Boolean = Boolean::and,
    block: T.(Array<out V?>) -> Unit
): T {
    if (values.isEmpty()) return this.apply { block(values) }
    var result = true
    for (value in values) {
        result = result.condition(value != null)
    }
    return if (result) this.apply { block(values) } else this
}
