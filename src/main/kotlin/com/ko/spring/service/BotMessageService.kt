package com.ko.spring.service

import com.ko.bot.bot.ReceiptAndTime
import com.ko.bot.message.MessageCenter
import com.ko.bot.utils.Utils
import com.ko.spring.utils.toHtml
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

@Service
class BotMessageService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired private lateinit var bot: Bot
    @Autowired private lateinit var messageCenter: MessageCenter

    private val sentMessages: MutableMap<Int, ReceiptAndTime>  = ConcurrentHashMap()
    private val lastMessages: MutableMap<Int, Message> = ConcurrentHashMap()

    data class MessageHashAndHtml(val hash: Int, val html: String)

    fun lastMessage(groupID: Long): MessageHashAndHtml? {
        val group = bot.getGroup(groupID) ?: return null
        val msg = messageCenter.lastMessage[groupID] ?: return null
        val hashCode = msg.hashCode()
        lastMessages[hashCode] = msg
        return MessageHashAndHtml(hashCode, msg.toHtml(group))
    }

    suspend fun repeat(groupID: Long, hash: Int): Int {
        val msg = lastMessages[hash] ?: return -1
        val group = bot.getGroup(groupID) ?: return -1
        val receipt = group.sendMessage(msg)

        logger.info("repeat group={}, message={}", groupID, msg.contentToString())

        val hashCode = receipt.hashCode()

        sentMessages[hashCode] = ReceiptAndTime(receipt)
        return hashCode
    }

    suspend fun sendText(groupID: Long, text: String, delay: Long = -1): Int {
        val group = bot.getGroup(groupID) ?: return -1
        return if (delay > 0) {
            GlobalScope.launch {
                delay(delay)
                group.sendMessage(text)
            }
            -100
        } else {
            val receipt = group.sendMessage(text)
            val hashCode = receipt.hashCode()
            sentMessages[hashCode] = ReceiptAndTime(receipt)
            hashCode
        }
    }

    suspend fun sendImgByFile(groupID: Long, file: File, r18: Boolean): Int {
        val group = bot.getGroup(groupID) ?: return -1
        return if (r18) {
            val img = ImageIO.read(file)
            Utils.r18Image(img)
            val receipt = group.sendMessage(group.uploadImage(Utils.bufferedImageToResource(img)))

            val hashCode = receipt.hashCode()
            sentMessages[hashCode] = ReceiptAndTime(receipt)
            hashCode
        } else {
            val receipt = group.sendMessage(group.uploadImage(file.toExternalResource()))
            val hashCode = receipt.hashCode()
            sentMessages[hashCode] = ReceiptAndTime(receipt)
            hashCode
        }
    }

    suspend fun sendByUrl(groupID: Long, url: String, r18: Boolean): Int {
        val group = bot.getGroup(groupID) ?: return -1
        val image = Utils.getBufferedImageFromUrl(url) ?: return -1
        if (r18) Utils.r18Image(image)
        val receipt = group.sendMessage(group.uploadImage(Utils.bufferedImageToResource(image)))
        val hashCode = receipt.hashCode()
        sentMessages[hashCode] = ReceiptAndTime(receipt)
        return hashCode
    }

    suspend fun recall(hash: Int) {
        val receipt = sentMessages[hash]?:return
        try {
            receipt.receipt.recall()
        } catch (e: Exception) {

        } finally {
            sentMessages.remove(hash)
        }
    }

    data class MessageAndTime(val hash: Int, val messageHtml: String, val time: String)

    private val dateFormat = SimpleDateFormat("HH:mm:ss")

    fun getSentMessages(groupID: Long): List<MessageAndTime> {
        return messageCenter.sentMessages.filter { it.receipt.target.id == groupID }.map {
            MessageAndTime(it.hash, it.receipt.toHtml(), dateFormat.format(Date(it.time)))
        }
    }

    suspend fun recallMessage(hash: Int, groupID: Long): Boolean {
        return try {
            val reps = messageCenter.sentMessages.filter { it.hash == hash && it.receipt.target.id == groupID }
            messageCenter.sentMessages -= reps
            reps.forEach { it.receipt.recall() }
            true
        } catch (e: Exception) {
            false
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    fun autoClean() {
        val now = System.currentTimeMillis()
        val l = lastMessages.size
        val s = sentMessages.size

        sentMessages.clear()
        lastMessages.clear()

        val before = messageCenter.sentMessages.size
        messageCenter.sentMessages.removeIf { now - it.time > 900_000 }

        logger.info("Clear lastMessages: {}, WebSentMessages: {}, BotSentMessages: {}", l - lastMessages.size, s - sentMessages.size, before - messageCenter.sentMessages.size)
    }
}