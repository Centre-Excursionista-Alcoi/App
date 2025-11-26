package org.centrexcursionistalcoi.app.security

object NIFValidation {
    fun validate(nif: String?): Boolean {
        nif ?: return false

        val upperNif = nif.trim().uppercase()

        // Comprobación de longitud
        if (upperNif.length != 9) return false

        // Patrón general DNI o NIE
        val regex = Regex("^[XYZ]?[0-9]{7,8}[A-Z]$")

        if (!regex.matches(upperNif)) return false

        // Separar número y letra de control
        val letraControl = upperNif.last()
        var numeroParte = upperNif.dropLast(1)

        // Sustitución para NIE
        numeroParte = when (numeroParte.first()) {
            'X' -> "0" + numeroParte.drop(1)
            'Y' -> "1" + numeroParte.drop(1)
            'Z' -> "2" + numeroParte.drop(1)
            else -> numeroParte
        }

        // Verificar que los números sean válidos
        val numero = numeroParte.toLongOrNull() ?: return false

        val letrasControl = "TRWAGMYFPDXBNJZSQVHLCKE"
        val letraEsperada = letrasControl[(numero % 23).toInt()]

        return letraEsperada == letraControl
    }
}
