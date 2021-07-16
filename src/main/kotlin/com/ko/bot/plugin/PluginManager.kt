package com.ko.bot.plugin

import com.ko.bot.inf.Catch
import com.ko.bot.inf.Plugin
import com.ko.bot.bot.GroupSettingsTool
import io.ktor.util.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.apache.commons.lang.StringUtils
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.annotation.PostConstruct
import kotlin.coroutines.CoroutineContext

@Component

class PluginManager {
    val pLogger: Logger = LoggerFactory.getLogger(PluginManager::class.java)
    val plugins: MutableMap<String, PluginLoader> = HashMap()

    @Autowired
    private lateinit var bot: Bot

    @Autowired
    private lateinit var groupSettingsTool: GroupSettingsTool

    data class PluginInfo(
        val name: String,
        val enabled: Map<Long, Boolean>
    ) {
        constructor(loader: PluginLoader) : this(loader.name, loader.enabled)
    }

    @PostConstruct
    fun init() {
        try {
            loadPlugins()
            bot.eventChannel.registerListenerHost(getHost())
        } catch (e: Exception) {
            pLogger.error(e)
        }
    }

    fun getHost(): SimpleListenerHost {
        return object : SimpleListenerHost() {
            @EventHandler
            @Throws(Exception::class)
            suspend fun onGroup(event: GroupMessageEvent): ListeningStatus {
                for (loader in plugins.values) {
                    loader.onGroupMessage(event)
                }
                return ListeningStatus.LISTENING
            }

            @EventHandler
            @Throws(Exception::class)
            suspend fun onFriend(event: FriendMessageEvent): ListeningStatus {
                for (loader in plugins.values) {
                    loader.onFriendMessage(event)
                }
                return ListeningStatus.LISTENING
            }

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                super.handleException(context, exception)
                pLogger.error(exception.toString(), exception)
            }
        }
    }

    fun getPluginList(gid: Long): String {
        if (plugins.isEmpty()) {
            return "未找到插件。"
        }
        val builder = StringBuilder("现有插件")
        for (name in plugins.keys) {
            builder.append("\n").append(name).append("  -  ").append(if (plugins[name]!!.isEnabled(gid)) "启用" else "禁用")
        }
        return builder.toString()
    }

    @Throws(Exception::class)
    fun dropPlugin(name: String): Boolean {
        val plugin: PluginLoader = plugins.remove(name) ?: return false
        plugin.drop()
        pLogger.info("drop {}", name)
        return true
    }

    @Throws(Exception::class)
    fun dropAll() {
        for (loader in plugins.values) {
            loader.drop()
        }
        plugins.clear()
    }

    @Throws(Exception::class)
    fun reloadAll() {
        dropAll()
        loadPlugins()
    }

    @Throws(Exception::class)
    fun reload(name: String): Boolean {
        if (!dropPlugin(name)) return false
        loadPlugins()
        return true
    }

    fun disablePlugin(name: String, gid: Long): Boolean {
        val p: PluginLoader = plugins[name] ?: return false
        p.disable(gid)

        val gs = groupSettingsTool.getSettings(gid)
        val ss = mutableListOf<String>()
        ss.addAll(gs.disabledPlugins.split(",").filter { it.isNotBlank() })
        if (name !in ss) ss.add(name)
        gs.disabledPlugins = StringUtils.join(ss, ",")
        groupSettingsTool.changeSettings(gs)

        return true
    }

    fun enablePlugin(name: String, gid: Long): Boolean {
        val p: PluginLoader = plugins[name] ?: return false
        p.enable(gid)

        val gs = groupSettingsTool.getSettings(gid)
        val ss = mutableListOf<String>()
        ss.addAll(gs.disabledPlugins.split(",").filter { it.isNotBlank() })
        ss.remove(name)
        gs.disabledPlugins = StringUtils.join(ss, ",")
        groupSettingsTool.changeSettings(gs)

        return true
    }

    @Throws(Exception::class)
    fun loadPlugins() {
        getPlugins().forEach { loadPlugin(it) }

        groupSettingsTool.getAllSettings().forEach { settings ->
            settings.disabledPlugins.split(",").filter { it.isNotBlank() }
                .forEach {
                    plugins[it]?.disable(settings.groupId)
                }
        }

        pLogger.info("插件加载完成，共{}个插件。", plugins.size)
    }

    @Throws(Exception::class)
    private fun getPlugins(): List<URL> {
        val f = File("./plugins")
        val files = f.listFiles { file: File ->
            file.name.endsWith(".jar")
        } ?: return emptyList()
        return files.map {
            URL("file:${it.path}")
        }
    }

    @Throws(Exception::class)
    fun loadPlugin(url: URL) {
        val urlCL = URLClassLoader(arrayOf(url), Plugin::class.java.classLoader)
        val reflections = Reflections(
            ConfigurationBuilder().setUrls(
                ClasspathHelper.forClassLoader(urlCL)
            ).addClassLoader(urlCL).addScanners(MethodAnnotationsScanner(), SubTypesScanner(true))
                .filterInputsBy { it.contains("plugin.koms") }
        )

        val classes = reflections.getSubTypesOf(Plugin::class.java)

        val plugin: Class<out Plugin?> = classes.iterator().next()
        val plugin1: Plugin? = plugin.getDeclaredConstructor().newInstance()
        val name: String = plugin1!!.getName()
        if (name.isBlank()) {
            plugin1.drop()
            throw RuntimeException("$url 无插件名！")
        }
        if (plugins.containsKey(name)) {
            plugin1.drop()
            return
        }
        val ms = reflections.getMethodsAnnotatedWith(Catch::class.java)
        if (ms == null || ms.isEmpty()) {
            plugin1.drop()
            throw RuntimeException("$name 无Catch注解的方法！")
        }
        val loader = PluginLoader(plugin1, name)

        plugin1.init(bot)

        ms.forEach { loader.scanMethod(it) }
        plugins[name] = loader
    }
}