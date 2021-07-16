package com.ko.bot.message

import com.ko.bot.bot.MyBot
import com.ko.bot.img.ImageSearchHelper
import com.ko.bot.inf.Catch
import com.ko.bot.bot.GroupSettingsTool
import com.ko.bot.utils.Settings
import com.ko.bot.utils.TextReader
import com.ko.bot.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dreamlu.mica.http.HttpRequest
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.FlashImage
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.toMessageChain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.imageio.ImageIO

@Component
class Misc {
    @Autowired
    private lateinit var myBot: MyBot

    @Autowired
    private lateinit var messageCenter: MessageCenter

    @Autowired
    private lateinit var imageSearchHelper: ImageSearchHelper

    @Autowired
    private lateinit var groupSettingsTool: GroupSettingsTool

    @Catch("#version")
    suspend fun version(group: Group) {
        group.sendMessage("KOMS BOT v1.4.0\nPOWERED BY Mirai 2.5.0 & SpringBoot 2.4.2\nHOME PAGE ${Settings.getString("bot.host")}")
    }

    @Catch("#recall")
    suspend fun recall(group: Group) {
        messageCenter.recallLast(group)
    }

    @Catch("^=.+", permission = Catch.SUPER_USER, listen = Catch.ON_FRIEND, removeRegexes = ["^="])
    suspend fun control(sender: Friend, cmd: String) {
        when (cmd) {
            "读取翻译" -> {
                imageSearchHelper.reload()
                sender.sendMessage("读取完成！")
                return
            }
            "读取文本" -> {
                TextReader.loadTexts()
                sender.sendMessage("完成！")
                return
            }
            else -> sender.sendMessage("未知指令。")
        }
    }

    @Catch("[\\s\\S]*@[\\s\\S]+", nologging = true)
    suspend fun atEvent(sender: Member, chain: MessageChain) {
        chain.firstOrNull { it is At && it.target == myBot.bot.id } ?: return
        val thing = chain.filter { it !is At }.toMessageChain().contentToString()
        val p = Pattern.compile(TextReader.getString("offense"))
        val matcher = p.matcher(thing)
        if (matcher.find()) {
            val reply = HttpRequest.get("https://nmsl.shadiao.app/api.php?level=min")
                .execute()
                .asString()
            sender.group.sendMessage(
                if (reply == null) TextReader.getText(
                    "offenseReply",
                    sender
                ) else At(sender).plus(reply)
            )
            return
        }
    }

    @Catch(".*机器人.*", nologging = true)
    suspend fun imNotBot(sender: Member, str: String) {
        val ts = TextReader.getStrings("ImNotBotTrigger", "-")
        for (t in ts) {
            if (str.contains(t)) {
                sender.group.sendMessage(TextReader.getRandomText("ImNotBot", "-"))
                break
            }
        }
    }

    @Catch("\\[闪照]")
    @Throws(IOException::class)
    suspend fun flashImage(group: Group, chain: MessageChain) {
        val img = (chain.firstOrNull { it is FlashImage } ?: return) as FlashImage
        group.sendMessage(img.image).recallIn(20_000)
        withContext(Dispatchers.IO) {
            ImageIO.write(
                Utils.getBufferedImageFromUrl(Utils.getImageURL(img.image)),
                "jpg",
                File("./image/flash_${img.image.imageId}.jpg")
            )
        }
    }

    @Catch("开启健全模式", permission = Catch.ADMIN)
    suspend fun pureModeOpen(group: Group) {
        val settings = groupSettingsTool.getSettings(group.id)
        if (settings.pureMode) {
            group.sendMessage("无需变更。")
        } else {
            settings.pureMode = true
            groupSettingsTool.changeSettings(settings)
            group.sendMessage("健全模式已开启。")
        }
    }

    @Catch("关闭健全模式", permission = Catch.ADMIN)
    suspend fun pureModeClose(group: Group) {
        val settings = groupSettingsTool.getSettings(group.id)
        if (!settings.pureMode) {
            group.sendMessage("无需变更。")
        } else {
            settings.pureMode = false
            groupSettingsTool.changeSettings(settings)
            group.sendMessage("健全模式已关闭。")
        }
    }

    @Catch("random")
    suspend fun random(group: Group) {
        group.sendMessage(Math.random().toString())
    }
}

