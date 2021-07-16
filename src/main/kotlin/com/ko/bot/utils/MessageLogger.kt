package com.ko.bot.utils

import io.ktor.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * 供消息处理类及插件使用的日志。可使用 [MessageLogger.logger] 调用更多日志方法。
 */
object MessageLogger {
    val logger: Logger = LoggerFactory.getLogger(MessageLogger::class.java)

    @JvmStatic
    fun info(text: String?, vararg objects: Any?) {
        logger.info(text, *objects)
    }

    @JvmStatic
    fun warn(text: String?, vararg objects: Any?) {
        logger.warn(text, objects)
    }

    @JvmStatic
    fun error(e: Throwable) {
        logger.error(e)
    }
}