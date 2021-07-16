package com.ko.bot.inf

import net.mamoe.mirai.event.events.MessageEvent

interface MessageFunction {
    suspend fun invoke(e: MessageEvent)

    val name: String
    val permission: Int
    val nologging: Boolean
}