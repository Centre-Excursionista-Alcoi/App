package org.centrexcursionistalcoi.app.validation

/**
 * @see <a href="https://emailregex.com">Source</a>
 */
private val emailRegex = "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])".toRegex()

val String.isValidEmail: Boolean
    get() = emailRegex.matches(this)

/**
 * Minimum eight characters, at least one letter and one number
 */
val String.isSafePassword: Boolean
    get() = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}\$".toRegex().matches(this)

private const val niePrefix = "XYZ"

private const val controlDigits = "TRWAGMYFPDXBNJZSQVHLCKE"

/**
 * Includes both DNI and NIE
 */
private val nifRegex = "^((\\d{8})|([$niePrefix]\\d{7}))[$controlDigits]\$".toRegex()

/**
 * @see <a href="https://www.interior.gob.es/opencms/es/servicios-al-ciudadano/tramites-y-gestiones/dni/calculo-del-digito-de-control-del-nif-nie/">Source</a>
 */
val String.isValidNif: Boolean
    get() {
        if (!nifRegex.matches(this)) return false

        val niePrefixIdx = niePrefix.indexOf(get(0))
        val number = if (niePrefixIdx >= 0) {
            val value = niePrefix[niePrefixIdx] + substring(1, 8)
            value.toInt()
        } else {
            substring(0, 8).toInt()
        }
        val mod = number % 23
        return controlDigits[mod] == last()
    }
