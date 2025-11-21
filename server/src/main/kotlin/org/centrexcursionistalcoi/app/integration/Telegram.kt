package org.centrexcursionistalcoi.app.integration

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ConfigProvider
import org.centrexcursionistalcoi.app.data.Post
import org.slf4j.LoggerFactory

object Telegram : ConfigProvider(), Closeable {
    private var bot: Bot? = null

    private val logger = LoggerFactory.getLogger(Telegram::class.java)

    private val botToken get() = getenv("TELEGRAM_API_KEY")
    private val chatId get() = getenv("TELEGRAM_CHAT_ID")?.toLongOrNull()?.let { ChatId.fromId(it) }

    fun init() {
        if (bot != null) return

        val botToken = botToken
        if (botToken == null) {
            logger.warn("Telegram bot token is not set. Telegram integration will be disabled.")
            return
        }
        if (chatId == null) {
            logger.warn("Telegram chat ID is not set or invalid. Telegram integration will be disabled.")
            return
        }

        bot = bot {
            token = botToken
        }
        bot?.startPolling()
    }

    override fun close() {
        bot?.close()
        bot = null
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        if (bot == null) return@launch
        block()
    }

    fun sendPost(post: Post) {
        val bot = bot ?: return

        bot.sendMessage(
            chatId = chatId!!,
            text = post.content,
        )
    }
}
