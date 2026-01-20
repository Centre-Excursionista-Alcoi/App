package org.centrexcursionistalcoi.app.security

open class ParametrizedPermission(
    /**
     * The permission string, replace parameters with `*`.
     */
    val permission: String
) {
    private val parametersCount = permission.count { it == '*' }

    /**
     * Generates the permission string by replacing the wildcards (`*`) with the provided parameters.
     * @param params The parameters to replace the wildcards. Will be converted to string using [Any.toString]. Cannot be blank.
     * @return The generated permission string.
     * @throws IllegalArgumentException if the number of provided parameters does not match the number of wildcards. Or if any parameter is blank.
     */
    operator fun invoke(vararg params: Any): String {
        require(params.size == parametersCount) { "Given parameters count (${params.size}) does not match the required amount ($parametersCount)" }

        var result = permission
        for (param in params) {
            val str = param.toString()
            if (str.isBlank()) {
                throw IllegalArgumentException("Parameter value cannot be blank")
            }
            result = result.replaceFirst("*", param.toString())
        }
        return result
    }

    companion object {
        /**
         * Converts a string to a [ParametrizedPermission].
         */
        fun String.parametrized(): ParametrizedPermission {
            return ParametrizedPermission(this@parametrized)
        }
    }
}
