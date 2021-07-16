package com.ko.bot.message

import com.ko.bot.bot.BotDBHelper
import com.ko.bot.entity.History
import com.ko.bot.entity.PixivImage
import com.ko.bot.exception.SendImageException
import com.ko.bot.img.HyperImageSearch
import com.ko.bot.img.SearchByImage
import com.ko.bot.img.SetuService
import com.ko.bot.inf.Catch
import com.ko.bot.utils.TextReader
import com.ko.bot.utils.Utils
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Setu {
    @Autowired
    private lateinit var setuService: SetuService
    @Autowired
    private lateinit var hyperImageSearch: HyperImageSearch
    @Autowired
    private lateinit var botDBHelper: BotDBHelper

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Catch("^来[点份张].*[色涩]图.*", removeRegexes = ["^来[点份张]", "[涩色]图.*"])
    suspend fun search(sender: Member, kw: String) {
        val banned: List<String> = TextReader.getStrings("bannedTags", "-")
        for (b in banned) {
            if (kw.contains(b)) {
                sender.group.sendMessage(At(sender).plus("不准$b！"))
                return
            }
        }
        sender.group.sendMessage(TextReader.getText("requireImageReply", sender))
        val cmd: String =
            SetuService.SEARCH + kw
        val res = sendImage(cmd, sender.group, true)
        botDBHelper.saveOrUpdateSomething(
            History(
                sender.nameCard,
                sender.id,
                sender.group.name,
                sender.group.id,
                cmd,
                "SEARCH",
                res.pid
            )
        )
    }

    @Catch("^来点.+", nologging = true, removeRegexes = ["^来点"])
    suspend fun searchByMapping(sender: Member, kw: String) {
        if (kw.matches(".*[色涩]图.*".toRegex())) {
            return
        }
        sendImage(SetuService.MAPPING + kw, sender.group, needMore = true, false)
    }

    @Catch("^[多再][来色涩]点|^不够[涩色]")
    suspend fun more(sender: Member) {
        val cmd: String = setuService.getMore(sender.group.id) ?: return
        sender.group.sendMessage(TextReader.getText("moreImageReply", sender))
        val res = sendImage(cmd, sender.group, false)
        botDBHelper.saveOrUpdateSomething(
            History(
                sender.nameCard,
                sender.id,
                sender.group.name,
                sender.group.id,
                cmd,
                "MORE",
                res.pid
            )
        )
    }

    @Catch("^来点推荐$|^推荐[色涩]图$")
    suspend fun recommend(sender: Member) {
        sender.group.sendMessage(TextReader.getText("recommendReply", sender))
        val res = sendImage(SetuService.RECOMMEND, sender.group, true)
        botDBHelper.saveOrUpdateSomething(
            History(sender.nameCard, sender.id, sender.group.name, sender.group.id, "RECOMMEND", res.pid)
        )
    }

    @Catch("^查图\\d+", removeRegexes = ["^查图"])
    suspend fun illustIdSearch(sender: Member, id: String) {
        sender.group.sendMessage(TextReader.getText("requireImageReply", sender))
        sendImage(
            SetuService.ILLUST + id,
            sender.group, false
        )
    }

    @Catch("^作者\\d+", removeRegexes = ["^作者"])
    suspend fun searchArtist(sender: Member, id: String) {
        sender.group.sendMessage(TextReader.getText("requireImageReply", sender))
        val cmd = SetuService.ARTIST + id
        val res = sendImage(cmd, sender.group, true)
        botDBHelper.saveOrUpdateSomething(
            History(
                sender.nameCard,
                sender.id,
                sender.group.name,
                sender.group.id,
                cmd,
                "ARTIST",
                res.pid
            )
        )
    }

    @Catch("^搜图[\\s\\S]+")
    suspend fun searchByImage(sender: Member, chain: MessageChain) {
        val images: List<Image> = chain.filterIsInstance<Image>()
        if (images.isEmpty()) return

        sender.group.sendMessage(TextReader.getText("requireImageReply", sender))
        for (image in images) {
            val res: Array<Array<String>?>? = SearchByImage.searchAscii2d(Utils.getImageURL(image))
            if (res == null) {
                sender.group.sendMessage("搜索失败！")
                return
            }
            try {
                withTimeout(30000L) {
                    val job1 = GlobalScope.launch {
                        try {
                            val resource: ExternalResource = Utils.getResourceFromUrl(res[0]!![0])
                            sender.group.sendMessage(
                                PlainText("ascii2d色合检索：\n").toMessageChain()
                                    .plus(sender.group.uploadImage(resource))
                                    .plus(
                                        """
                                
                                链接: ${res[0]!![1]}
                                """.trimIndent()
                                    )
                            )
                        } catch (e: Exception) {
                            sender.group.sendMessage(
                                """
                        ascii2d色合检索：
                        图片获取失败！
                        链接: ${res[0]!![1]}
                        """.trimIndent()
                            )
                        }
                    }
                    if (res[1] == null) {
                        sender.group.sendMessage("特征搜索失败！")
                    } else {
                        val job2 = GlobalScope.launch {
                            try {
                                val resource: ExternalResource = Utils.getResourceFromUrl(res[1]!![0])
                                sender.group.sendMessage(
                                    PlainText("ascii2d特征检索：\n").toMessageChain()
                                        .plus(sender.group.uploadImage(resource))
                                        .plus(
                                            """
                                    
                                    链接: ${res[1]!![1]}
                                    """.trimIndent()
                                        )
                                )
                            } catch (e: Exception) {
                                sender.group.sendMessage(
                                    """
                            ascii2d特征检索：
                            图片获取失败！
                            链接: ${res[1]!![1]}
                            """.trimIndent()
                                )
                            }
                        }
                        job2.join()
                    }
                    job1.join()
                }
            } catch (e: TimeoutCancellationException) {
                sender.group.sendMessage("超时！")
            } catch (e: Exception) {
                logger.error(e)
            }

        }
    }

    @Catch("^订阅画师\\d+", removeRegexes = ["^订阅画师"])
    suspend fun subscribe(sender: Member, id: String) {
        val uid = id.toLong()
        sender.group.sendMessage("处理中。。。")
        val check = hyperImageSearch.checkArtist(uid)
        if (check == null) {
            sender.group.sendMessage("失败：uid无效或网络问题！")
            return
        }
        sender.group.sendMessage("订阅${check.name}(${check.uid})完成。")
        hyperImageSearch.subscribe(sender.group.id, uid)
    }

    @Catch("^取消订阅\\d+", removeRegexes = ["^取消订阅"])
    suspend fun unsubscribe(sender: Member, id: String) {
        val uid = id.toLong()
        hyperImageSearch.subscribe(sender.group.id, uid, true)
        sender.group.sendMessage("已取消。")
    }

    suspend fun sendImage(cmd: String, group: Group, needMore: Boolean, sendFailMsg: Boolean = true): PixivImage {
        val image: PixivImage = setuService.cmd(cmd, group.id)
        if (image.pid < 0) {
            if (sendFailMsg && image != PixivImage.INTENTIONAL_ERROR)
                group.sendMessage(TextReader.getText("sendImageFailed").plus(setuService.getErrorString(image.pid)))
        } else {
            sendWithRetry(cmd, image, group)
            if (needMore) setuService.putMore(group.id, cmd)
        }
        return image
    }

    suspend fun sendWithRetry(cmd: String, image: PixivImage, group: Group, time: Int = 0) {
        if (time >= 5) {
            return
        } else {
            try {
                setuService.sendImage(image, group)
            } catch (e: SendImageException) {
                logger.error(e)
                val newImg = setuService.cmd(cmd, group.id)
                if (newImg.pid == image.pid) {
                    group.sendMessage("${TextReader.getText("sendImageFailed")}${e.message}")
                    return
                }
                logger.info("Retry send image: cmd = {}, time = {}", cmd, time)
                sendWithRetry(cmd, newImg, group, time + 1)
            }
        }
    }
}
