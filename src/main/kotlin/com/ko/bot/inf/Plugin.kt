package com.ko.bot.inf

import net.mamoe.mirai.Bot
import java.lang.Exception

/**
 * 插件信息类要实现的接口。
 */
interface Plugin {
    /**
     * 插件加载时调用的方法。
     */
    @Throws(Exception::class)
    fun init(bot: Bot)

    /**
     * 插件名字，返回的值作为此插件唯一标识符。
     */
    fun getName(): String

    /**
     * 插件卸载时调用的方法。
     */
    @Throws(Exception::class)
    fun drop()
}