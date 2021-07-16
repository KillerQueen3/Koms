package com.ko.bot.plugin

import com.ko.bot.exception.CatcherIllegalException
import com.ko.bot.inf.Catch
import com.ko.bot.inf.Plugin
import com.ko.bot.message.MessageCenter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.kotlinFunction

class PluginLoader(private val plugin: Plugin, val name: String)  {
    val enabled: MutableMap<Long, Boolean> = ConcurrentHashMap()
    private val logger: Logger = LoggerFactory.getLogger(name)

    private val onGroup = mutableMapOf<String, PluginFunction>()
    private val onFriend = mutableMapOf<String, PluginFunction>()

    @Throws(Exception::class)
    fun drop() {
        plugin.drop()
    }

    fun enable(groupID: Long) {
        enabled[groupID] = true
    }

    fun disable(groupID: Long) {
        enabled[groupID] = false
    }

    fun isEnabled(groupID: Long): Boolean {
        return enabled.getOrDefault(groupID, true)
    }

    fun scanMethod(method: Method) {
        val c: Catch = method.getAnnotation(Catch::class.java)
        val f = method.kotlinFunction ?: throw CatcherIllegalException("$method 无法转换为kotlin函数！")
        logger.debug("scanning $f")
        if (c.entry.isEmpty()) {
            throw CatcherIllegalException("${method.name} 入口文本长度为0！")
        }

        if (f.instanceParameter != null)
            throw CatcherIllegalException("${method.name} 存在实例参数！")

        if (c.listen == Catch.ON_GROUP) onGroup[c.entry] = PluginFunction(f, c)
        else if (c.listen == Catch.ON_FRIEND) onFriend[c.entry] = PluginFunction(f, c)
    }

    suspend fun onGroupMessage(event: GroupMessageEvent) {
        val r = event.message.contentToString()
        coroutineScope {
            for ((regex, pluginFunction) in onGroup) {
                if (r.matches(Regex(regex)) && enabled[event.group.id] != false) {
                    launch {
                        MessageCenter.onGroupMessageEvent(event, pluginFunction, logger)
                    }
                }
            }
        }
    }

    suspend fun onFriendMessage(event: FriendMessageEvent) {
        val r =  event.message.contentToString()
        coroutineScope {
            for ((regex, pluginFunction) in onFriend) {
                if (r.matches(Regex(regex))) {
                    launch {
                       MessageCenter.onFriendMessageEvent(event, pluginFunction, logger)
                    }
                }
            }
        }
    }
}