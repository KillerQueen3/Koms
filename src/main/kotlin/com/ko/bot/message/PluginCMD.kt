package com.ko.bot.message

import com.ko.bot.plugin.PluginManager
import com.ko.bot.inf.Catch
import io.ktor.util.*
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageChain
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class PluginCMD {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var pluginManager: PluginManager

    @Catch("^卸载\\S+", permission = Catch.SUPER_USER, listen = Catch.ON_FRIEND, removeRegexes = ["^卸载"])
    suspend fun drop(sender: Friend, name: String) {
        var msg = "卸载出错！"
        try {
            msg = if (pluginManager.dropPlugin(name)) "成功！" else "未找到$name"
        } catch (e: Exception) {
            logger.error(e)
        }
        sender.sendMessage(msg)
    }

    @Catch("^插件列表$")
    suspend fun list(sender: Member) {
        sender.group.sendMessage(pluginManager.getPluginList(sender.group.id))
    }

    @Catch("^加载插件$", permission = Catch.SUPER_USER, listen = Catch.ON_FRIEND)
    @Throws(Exception::class)
    suspend fun load(sender: Friend) {
        try {
            pluginManager.loadPlugins()
            sender.sendMessage("完成。")
        } catch (e: Exception) {
            sender.sendMessage("加载插件时发生错误！信息：" + e.message)
            throw e
        }
    }

    @Catch("^重载插件$", permission = Catch.SUPER_USER, listen = Catch.ON_FRIEND)
    @Throws(Exception::class)
    suspend fun reload(sender: Friend) {
        try {
            pluginManager.reloadAll()
            sender.sendMessage("完成。")
        } catch (e: Exception) {
            sender.sendMessage("加载插件时发生错误！信息：" + e.message)
            throw e
        }
    }

    @Catch("^重新加载.+", permission = Catch.SUPER_USER, listen = Catch.ON_FRIEND, removeRegexes = ["^重新加载"])
    suspend fun reloadOne(sender: Friend, name: String) {
        try {
            if (pluginManager.reload(name)) {
                sender.sendMessage("完成。")
            } else {
                sender.sendMessage("未找到$name")
            }
        } catch (e: Exception) {
            sender.sendMessage("加载插件时发生错误！信息：" + e.message)
            throw e
        }
    }

    @Catch("^=禁用\\S+", permission = Catch.ADMIN, removeRegexes = ["^=禁用"])
    suspend fun disable(sender: Member, name: String) {
        sender.group.sendMessage(if (pluginManager.disablePlugin(name, sender.group.id)) "成功！" else "未找到$name")
    }

    @Catch("^=启用\\S+", permission = Catch.ADMIN, removeRegexes = ["^=启用"])
    suspend fun enable(sender: Member, name: String) {
        sender.group.sendMessage(if (pluginManager.enablePlugin(name, sender.group.id)) "成功！" else "未找到$name")
    }
}



