package com.ko.bot.plugin

import com.ko.bot.inf.Catch
import com.ko.bot.inf.MessageFunction
import com.ko.bot.message.MessageCenter
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy

class PluginFunction(private val function: KFunction<*>, private val catch: Catch) : MessageFunction {
    @Throws(Exception::class)
    override suspend fun invoke(e: MessageEvent) {
        val map = MessageCenter.getParamsMap(function, e, catch.removeRegexes)
        function.callSuspendBy(map)
    }

    override val permission = catch.permission
    override val nologging = catch.nologging

    override val name = function.name
}