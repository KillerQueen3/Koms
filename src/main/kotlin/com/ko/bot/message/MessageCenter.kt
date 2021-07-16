package com.ko.bot.message

import com.ko.bot.bot.MyBot
import com.ko.bot.bot.ReceiptAndTime
import com.ko.bot.exception.CatcherIllegalException
import com.ko.bot.inf.Catch
import com.ko.bot.inf.MessageFunction
import com.ko.bot.utils.Settings
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.PostConstruct
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

@Component
class MessageCenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun getParamsMap(
            function: KFunction<*>,
            e: MessageEvent,
            removeRegex: Array<String>
        ): MutableMap<KParameter, Any> {
            val map = mutableMapOf<KParameter, Any>()

            function.valueParameters.forEach {
                when {
                    it.type.isSubtypeOf(User::class.createType()) -> {
                        map[it] = e.sender
                    }
                    it.type.isSubtypeOf(Message::class.createType()) -> {
                        map[it] = e.message
                    }
                    it.type == String::class.createType() -> {
                        var str = e.message.contentToString()
                        removeRegex.forEach { regex -> str = str.replace(regex.toRegex(), "") }
                        map[it] = str
                    }
                    it.type.isSubtypeOf(MessageEvent::class.createType()) -> {
                        map[it] = e
                    }
                    it.type.isSubtypeOf(Bot::class.createType()) -> {
                        map[it] = e.bot
                    }
                    it.type == Group::class.createType() -> {
                        if (e !is GroupMessageEvent) {
                            throw CatcherIllegalException("${function.name} 非群消息事件调用了群变量！")
                        }
                        map[it] = e.group
                    }
                    else -> {
                        throw CatcherIllegalException("${function.name} 有不支持类型 ${it.type} 的参数 ${it.name}")
                    }
                }
            }

            return map
        }

        suspend fun onGroupMessageEvent(event: GroupMessageEvent, function: MessageFunction, logger: Logger) {
            if (event.sender.id.toString() !in Settings.getList("bot.superUser")) {
                if (function.permission == Catch.SUPER_USER
                    || (function.permission == Catch.ADMIN && event.sender.permission == MemberPermission.MEMBER)
                    || (function.permission == Catch.OWNER && event.sender.permission != MemberPermission.OWNER)
                ) {
                    return
                }
            }
            try {
                if (!function.nologging) {
                    logger.info(
                        "Group message - group: {}({}), member: {}({}), message: {}, method: {}",
                        event.sender.group.name,
                        event.sender.group.id,
                        event.sender.nameCardOrNick,
                        event.sender.id,
                        event.message.contentToString(),
                        function.name
                    )
                }
                function.invoke(event)
            } catch (botIsMuted: BotIsBeingMutedException) {
                logger.warn(botIsMuted.message)
            } catch (e: java.lang.Exception) {
                logger.error(e)
            }
        }

        suspend fun onFriendMessageEvent(event: FriendMessageEvent, function: MessageFunction, logger: Logger) {
            if (function.permission == Catch.SUPER_USER && event.sender.id.toString() !in Settings.getList("bot.superUser")) {
                event.sender.sendMessage("需要超级用户权限。")
                return
            }
            try {
                if (!function.nologging) {
                    logger.info(
                        "Friend message - friend: {}({}), message: {}, method: {}",
                        event.sender.nick, event.sender.id, event.message.contentToString(), function.name
                    )
                }
                function.invoke(event)
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }

    val sentMessages: MutableList<ReceiptAndTime> = CopyOnWriteArrayList()

    suspend fun recallLast(group: Group) {
        val msg = sentMessages.firstOrNull{ it.receipt.target.id == group.id }?: return
        try {
            msg.receipt.recall()
            sentMessages.remove(msg)
        } catch (e: Exception) {

        }
    }

    private class LocalFunction(
        private val function: KFunction<*>,
        private val catch: Catch,
        private val instance: Any
    ) : MessageFunction {
        @Throws(Exception::class)
        override suspend fun invoke(e: MessageEvent) {
            val map = getParamsMap(function, e, catch.removeRegexes)
            map[function.instanceParameter!!] = instance
            function.callSuspendBy(map)
        }

        override val permission = catch.permission
        override val nologging = catch.nologging

        override val name: String = function.name
    }

    private val onGroup: MutableMap<String, LocalFunction> = HashMap()
    private val onFriend: MutableMap<String, LocalFunction> = HashMap()
    val lastMessage: MutableMap<Long, MessageChain> = ConcurrentHashMap()

    @Autowired
    private lateinit var misc: Misc

    @Autowired
    private lateinit var pluginCMD: PluginCMD

    @Autowired
    private lateinit var setu: Setu

    fun scanLocalClass(instance: Any) {
        instance::class.functions.forEach {
            val catch = it.findAnnotation<Catch>()
            if (catch != null) {
                if (catch.listen == Catch.ON_GROUP) {
                    onGroup[catch.entry] = LocalFunction(it, catch, instance)
                } else if (catch.listen == Catch.ON_FRIEND) {
                    onFriend[catch.entry] = LocalFunction(it, catch, instance)
                }
            }
        }
    }

    suspend fun onGroupMessage(event: GroupMessageEvent) {
        lastMessage[event.group.id] = event.message
        val r = event.message.contentToString()
        coroutineScope {
            for ((regex, localFunction) in onGroup) {
                if (r.matches(Regex(regex))) {
                    launch {
                        onGroupMessageEvent(event, localFunction, logger)
                    }
                }
            }
        }
    }

    suspend fun onFriendMessage(event: FriendMessageEvent) {
        val r = event.message.contentToString()
        coroutineScope {
            for ((regex, localFunction) in onFriend) {
                if (r.matches(Regex(regex))) {
                    launch {
                        onFriendMessageEvent(event, localFunction, logger)
                    }
                }
            }
        }
    }

    @Autowired
    private lateinit var myBot: MyBot

    @PostConstruct
    fun registerBot() {
        scanDefaultClasses()
        myBot.bot.eventChannel.registerListenerHost(object : SimpleListenerHost() {
            @EventHandler
            @Throws(Exception::class)
            suspend fun onGroupMessageEvent(event: GroupMessageEvent): ListeningStatus {
                onGroupMessage(event)
                return ListeningStatus.LISTENING
            }

            @EventHandler
            @Throws(Exception::class)
            suspend fun onFriendMessageEvent(event: FriendMessageEvent): ListeningStatus {
                onFriendMessage(event)
                return ListeningStatus.LISTENING
            }

            @EventHandler
            @Throws(Exception::class)
            fun onMessagePreSendEvent(event: MessagePostSendEvent<Group>): ListeningStatus {
                val receipt = event.receipt
                if (receipt != null && receipt.isToGroup)
                    sentMessages.add(
                        0,
                        ReceiptAndTime(receipt)
                    )
                return ListeningStatus.LISTENING
            }

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                super.handleException(context, exception)
                logger.error(exception)
            }
        })

    }

    fun scanDefaultClasses() {
        scanLocalClass(misc)
        scanLocalClass(pluginCMD)
        scanLocalClass(setu)
        logger.info("默认指令：共读取 ${onGroup.size} 条群聊指令和 ${onFriend.size} 条私聊指令。")
    }
}