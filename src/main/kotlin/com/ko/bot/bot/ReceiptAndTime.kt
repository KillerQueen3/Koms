package com.ko.bot.bot

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.MessageReceipt

data class ReceiptAndTime(val hash: Int, val receipt: MessageReceipt<Group>, val time: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptAndTime

        if (hash != other.hash) return false
        if (time != other.time) return false

        return true
    }

    constructor(receipt: MessageReceipt<Group>): this(receipt.hashCode(), receipt, System.currentTimeMillis())

    override fun hashCode(): Int {
        var result = hash
        result = 31 * result + time.hashCode()
        return result
    }
}