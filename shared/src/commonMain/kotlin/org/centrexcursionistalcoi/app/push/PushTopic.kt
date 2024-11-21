package org.centrexcursionistalcoi.app.push

object PushTopic {
    @Deprecated("Use device IDs")
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
