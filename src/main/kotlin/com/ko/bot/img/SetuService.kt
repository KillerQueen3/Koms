package com.ko.bot.img

import com.ko.bot.entity.PixivImage
import com.ko.bot.exception.SendImageException
import com.ko.bot.bot.GroupSettingsTool
import com.ko.bot.utils.Settings
import com.ko.bot.utils.TextReader
import com.ko.bot.utils.Utils
import io.ktor.util.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.toMessageChain
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * 图片指令处理工具类。
 */
@Service
class SetuService {
    companion object {
        const val SEARCH = "search "
        const val RECOMMEND = "recommend"
        const val ILLUST = "illust "
        const val ARTIST = "artist "
        const val MAPPING = "mapping "
    }

    private val logger = LoggerFactory.getLogger(SetuService::class.java)
    private val before: MutableMap<Long, String?> = ConcurrentHashMap()

    @Autowired
    private lateinit var imageSearch: ImageSearch
    @Autowired
    private lateinit var imageSearchHelper: ImageSearchHelper
    @Autowired
    private lateinit var hyperImageSearch: HyperImageSearch
    @Autowired
    private lateinit var groupSettingsTool: GroupSettingsTool


    @PostConstruct
    fun init() {
        groupSettingsTool.getAllSettings().forEach {
            before[it.groupId] = it.moreCmd
        }
    }

    /**
     * 得到错误信息文本。
     * @param code 错误码
     * @return 错误信息文本，来自message.json
     * @see [PixivImage]
     */
    fun getErrorString(code: Long): String {
        return when (code.toInt()) {
            -1 -> TextReader.getString("noMoreImageError")
            -2 -> TextReader.getString("apiError")
            -3 -> TextReader.getString("timeOutError")
            -4 -> TextReader.getString("noSearchResult")
            -5 -> TextReader.getString("noArtistIllust")
            -6 -> TextReader.getString("noSuchIllust")
            -7 -> TextReader.getString("noSingleImage")
            -8 -> TextReader.getString("noPureImage")
            else -> TextReader.getString("unknownError")
        }
    }

    /**
     * 处理指令。
     * @param cmd 指令
     * @param groupID 发起群号
     * @return 返回的图片信息，或错误信息。
     */
    suspend fun cmd(cmd: String, groupID: Long): PixivImage {
        var cmd1 = cmd
        return try {
            if (cmd1.contains(SEARCH)) {
                cmd1 = cmd1.replaceFirst(SEARCH.toRegex(), "")
                val pureMode = groupSettingsTool.getSettings(groupID).pureMode
                if (pureMode && cmd1.contains("[Rr]-?18".toRegex())) return PixivImage.NO_PURE_IMAGE
                if (cmd1.replace(" ".toRegex(), "").isEmpty()) {
                    imageSearch.getSetuInfo(pureMode)
                } else {
                    imageSearch.getSetuInfo(groupID, cmd1, imageSearchHelper.getTrans(cmd1), pureMode = pureMode)
                }
            } else if (cmd1 == RECOMMEND) {
                imageSearch.recommend(groupSettingsTool.getSettings(groupID).pureMode)
            } else if (cmd1.contains(ILLUST)) {
                val id = cmd1.replace(ILLUST.toRegex(), "").toLong()
                imageSearch.getIllust(id)
            } else if (cmd1.contains(ARTIST)) {
                val id = cmd1.replace(ARTIST, "").toLong()
                imageSearch.searchByArtist(groupID, id)
            } else if (cmd1.contains(MAPPING)) {
                val tag =
                    hyperImageSearch.getMappedTag(cmd1.replace(MAPPING, "")) ?: return PixivImage.INTENTIONAL_ERROR
                imageSearch.getSetuInfo(
                    groupID, tag, imageSearchHelper.getTrans(tag),
                   // removePlural = false,
                    pureMode = groupSettingsTool.getSettings(groupID).pureMode
                )
            } else {
                PixivImage.UNKNOWN_ERROR
            }
        } catch (e: Exception) {
            logger.error(e)
            PixivImage.UNKNOWN_ERROR
        }
    }

    /**
     * 向某群发送 [image] 包含的图片及其信息。
     * @param image 要发送的图片信息
     * @param group 目标群
     * @param errorMessage 是否发送错误信息。
     * @throws SendImageException 由 [Utils.getBufferedImageFromUrl] 及 [Utils.getResourceFromUrl] 抛出。
     */
    suspend fun sendImage(image: PixivImage, group: Group, errorMessage: Boolean = true) {
        if (image.pid < 0) return
        val url: String = image.url ?: return
        if (image.originalUrl == null) {
            image.originalUrl = image.url
        }
        logger.info("Send image: {}({}) to {}({})", image.title, image.pid, group.name, group.id)
        try {
            if (!image.r18) {
                Utils.getResourceFromUrl(image.url!!).use {
                    group.sendMessage(group.uploadImage(it).plus(image.infoString()))
                }
            } else {
                if (Settings.getBoolean("pixiv.r18")) {
                    val bufferedImage = Utils.getBufferedImageFromUrl(url) ?: throw NullPointerException()
                    Utils.r18Image(bufferedImage)

                    Utils.bufferedImageToResource(bufferedImage).use {
                        group.sendMessage(group.uploadImage(it).plus(image.infoString()))
                    }

                } else {
                    File("./resource/h.png").toExternalResource().use {
                        group.sendMessage(group.uploadImage(it).plus(image.infoString()))
                    }
                }
            }
            if (image.p > 1)
                group.sendMessage("可通过 ${Settings.getString("bot.host")}/pixiv?type=id&thing=${image.pid} 查看全部图片。")
        } catch (e: SendImageException) {
            throw e
        } catch (e: Exception) {
            if (errorMessage)
                group.sendMessage(TextReader.getText("sendImageFailed").toString() + "链接: " + image.originalUrl)
            logger.error(e)
        }
    }


    /**
     * 得到某群的上一条指令，若无，返回null。
     */
    @Nullable
    fun getMore(groupID: Long): String? {
        return before.getOrDefault(groupID, null)
    }

    /**
     * 存储上一条指令的信息。
     */
    fun putMore(groupID: Long, cmd: String) {
        before.remove(groupID)
        before[groupID] = cmd
        groupSettingsTool.setMore(groupID, cmd)
    }
}