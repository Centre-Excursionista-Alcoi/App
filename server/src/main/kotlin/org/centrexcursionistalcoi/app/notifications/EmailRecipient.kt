package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail

/**
 * Who an email is sent to, and the language it is sent in.
 */
class EmailRecipient(val email: MailerSendEmail, val locale: Locale) {
    companion object {
        /**
         * The recipient for [user], in their language, [locale].
         */
        fun of(user: UserReferenceEntity, locale: Locale): EmailRecipient =
            EmailRecipient(Database { MailerSendEmail(user.email, user.fullName) }, locale)

        /**
         * The admins, and the members of any of [groups], each in their own language.
         */
        fun staff(vararg groups: String): List<EmailRecipient> = Database {
            UserReferenceEntity.all()
                .filter { user -> ADMIN_GROUP_NAME in user.groups || groups.any { it in user.groups } }
                .map { of(it, it.emailLocale()) }
        }
    }
}

/**
 * The language to send emails to this user in: the one they use the app in, or English if it isn't known yet.
 */
fun UserReferenceEntity.emailLocale(): Locale = Database { UserPreferenceStore[sub.value, UserPreferenceKey.Language] } ?: Locale.ENGLISH
