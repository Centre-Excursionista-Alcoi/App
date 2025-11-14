package org.centrexcursionistalcoi.app.utils

class Choice<T>(val a: T, val b: T) {
    infix fun chooseBy(condition: Boolean): T = if (condition) a else b
}

infix fun <T> T.or(other: T): Choice<T> = Choice(this, other)
