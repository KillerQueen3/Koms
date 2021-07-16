package com.ko.bot.bot

import com.ko.bot.exception.CatcherIllegalException
import com.ko.bot.utils.Settings
import com.ko.bot.utils.TextReader
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.BotConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PostConstruct
import kotlin.system.exitProcess

@Component
class MyBot {
    val logger: Logger = LoggerFactory.getLogger(MyBot::class.java)

    var bot: Bot = BotFactory.INSTANCE.newBot(
        Settings.getLong("bot.qq"),
        Settings.getString("bot.password"),
        object : BotConfiguration() {
            init {
                fileBasedDeviceInfo("deviceInfo.json")
                protocol = MiraiProtocol.ANDROID_PHONE
                redirectBotLogToDirectory(File("./log"))
                redirectNetworkLogToDirectory(File("./log"))
            }
        })

    @Bean
    fun getMBot() = bot

    var startTime: Long = 0
    var ready = false

    suspend fun login(): Boolean {
        return try {
            bot.login()
            startTime = System.currentTimeMillis()
            logger.info("LOGIN")
            ready = true
            true
        } catch (e: LoginFailedException) {
            e.printStackTrace()
            false
        }
    }

    fun getTime(): String {
        val totalMs = System.currentTimeMillis() - startTime
        var s = totalMs / 1000
        var min = s / 60
        s %= 60
        var hr = min / 60
        min %= 60
        val day = hr / 24
        hr %= 24
        return "$day 日 $hr 小时 $min 分 $s 秒"
    }

    @PostConstruct
    fun runBot() {
        try {
            TextReader.loadTexts()
        } catch (e: CatcherIllegalException) {
            e.printStackTrace()
            exitProcess(-1)
        }

        runBlocking {
            if (!login()) {
                exitProcess(-1)
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            bot.close(null)
            logger.info("CLOSED")
        })
    }
}


