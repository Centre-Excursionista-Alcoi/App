package org.centrexcursionistalcoi.app.utils

private val tildesPairs = mapOf(
    'Á' to 'A',
    'À' to 'A',
    'Ä' to 'A',
    'Â' to 'A',
    'á' to 'a',
    'à' to 'a',
    'ä' to 'a',
    'â' to 'a',
    'É' to 'E',
    'È' to 'E',
    'Ë' to 'E',
    'Ê' to 'E',
    'é' to 'e',
    'è' to 'e',
    'ë' to 'e',
    'ê' to 'e',
    'Í' to 'I',
    'Ì' to 'I',
    'Ï' to 'I',
    'Î' to 'I',
    'í' to 'i',
    'ì' to 'i',
    'ï' to 'i',
    'î' to 'i',
    'Ó' to 'O',
    'Ò' to 'O',
    'Ö' to 'O',
    'Ô' to 'O',
    'ó' to 'o',
    'ò' to 'o',
    'ö' to 'o',
    'ô' to 'o',
    'Ú' to 'U',
    'Ù' to 'U',
    'Ü' to 'U',
    'Û' to 'U',
    'ú' to 'u',
    'ù' to 'u',
    'ü' to 'u',
    'û' to 'u',
    'Ñ' to 'N',
    'ñ' to 'n',
)

fun CharSequence.unaccent(): String {
    val sb = StringBuilder(this.length)
    for (char in this) {
        sb.append(tildesPairs[char] ?: char)
    }
    return sb.toString()
}
