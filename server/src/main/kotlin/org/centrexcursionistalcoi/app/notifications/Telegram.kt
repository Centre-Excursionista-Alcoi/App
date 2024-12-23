package org.centrexcursionistalcoi.app.notifications

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.types.RawChatId
import org.slf4j.LoggerFactory

object Telegram {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var bot: TelegramBot

    suspend fun initialize() {
        val token: String? = System.getenv("TELEGRAM_BOT")
        if (token == null) {
            logger.warn("TELEGRAM_BOT not set. Won't send notifications through Telegram.")
            return
        }

        logger.info("Initializing Telegram bot...")
        bot = telegramBot(token)
        RawChatId(0)
        logger.info(bot.getMe().toString())
    }
}
