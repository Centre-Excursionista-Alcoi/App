package org.centrexcursionistalcoi.app.push

object PushTopic {
    fun topic(email: String): String {
        val prefix = email
            .substringBefore('@')
            .lowercase()
            .replace('.', '_')
        val suffix = email
            .substringAfter('@')
            .hashCode()

        return "$prefix$suffix"
    }
}
