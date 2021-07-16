package com.ko.bot.img

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.ko.bot.entity.PixivImage
import com.ko.bot.entity.SearchHistory
import com.ko.bot.entity.SearchHistoryKey
import com.ko.bot.exception.SendImageException
import com.ko.bot.bot.BotDBHelper
import com.ko.bot.utils.Settings
import com.ko.spring.utils.toMd5
import io.ktor.util.*
import kotlinx.coroutines.*
import net.dreamlu.mica.http.HttpRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import javax.annotation.PostConstruct
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

/**
 * 图片搜索工具类。
 */
@Component
class ImageSearch {
    private val logger = LoggerFactory.getLogger(ImageSearch::class.java)

    companion object {
        private val U_USERS = intArrayOf(1000, 5000, 10000, 20000)
    }

    private val SETU_FROM_LOLICON: MutableList<PixivImage> = CopyOnWriteArrayList()
    private val RECOMMEND: MutableList<PixivImage> = CopyOnWriteArrayList()

    @Autowired
    private lateinit var imageSearchHelper: ImageSearchHelper

    @Autowired
    private lateinit var botDBHelper: BotDBHelper

    @PostConstruct
    fun init() {
        logger.info("正在初始化。。。如遇报错请检查网络及api设置！")
        try {
            pixivLogin()
            addRecommend()
            addSetu()
        } catch (e: Exception) {
            logger.error(e)
        }
        logger.info("初始化完成。")
    }

    private fun getPixivInfoApi(): String {
        return "http://${Settings.getString("pixiv.host")}:${Settings.getString("pixiv.port")}/pixiv/"
    }

    private fun addSetu() {
        val url = "https://api.lolicon.app/setu/?size1200=&num=100&r18=${
            if (Settings.getBoolean("pixiv.r18")) "2" else "0"
        }"
        logger.info("Getting {}", url)
        val t = HttpRequest.get(url).retry(3, 2000).execute().asString()
        val jsonObject = JsonParser.parseString(t) as JsonObject
        if (jsonObject["code"].asInt == 0) {
            val info = jsonObject.getAsJsonArray("data").asJsonArray
            val res: List<PixivImage> =
                Gson().fromJson<List<PixivImage>>(info, object : TypeToken<List<PixivImage?>?>() {}.type)
                    ?: return
            res.forEach {
                val img = getIllust(it.pid)
                if (img.pid > 0)
                    SETU_FROM_LOLICON.add(img)
            }
            logger.info("Add Lolicon Setu, size = {}", SETU_FROM_LOLICON.size)
        } else {
            logger.warn("LOLICON API ERROR! RESPONSE: {}", t)
        }
    }

    /**
     * 从Lolicon api得到的图片信息。
     * @param pureMode 健全模式。
     */
    fun getSetuInfo(pureMode: Boolean = false): PixivImage {
        if (SETU_FROM_LOLICON.isEmpty()) {
            addSetu()
        }
        if (SETU_FROM_LOLICON.isEmpty()) {
            return PixivImage.API_ERROR
        } else {
            if (SETU_FROM_LOLICON.size == 1) {
                GlobalScope.launch { addSetu() }
            }

            if (pureMode) {
                var pure = SETU_FROM_LOLICON.filter { !it.r18 }
                if (pure.isEmpty()) {
                    addSetu()
                    pure = SETU_FROM_LOLICON.filter { !it.r18 }
                }
                if (pure.isEmpty()) return PixivImage.NO_PURE_IMAGE
                val res = pure.first()
                SETU_FROM_LOLICON.remove(res)
                return res
            }
            return SETU_FROM_LOLICON.removeAt(0)
        }
    }

    fun pixivLogin() {
        val url = "${getPixivInfoApi()}token?token=${Settings.getString("pixiv.token")}"
        try {
            val resp = HttpRequest.post(url).retry(3, 2000).execute().asString()
            logger.debug("PIXIV LOGIN - response: $resp")
            val respJson = JsonParser.parseString(resp) as JsonObject
            if (respJson["code"].asInt != 200) {
                logger.warn(respJson["message"].asString)
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    private fun addRecommend() {
        val url = "${getPixivInfoApi()}recommend"
        try {
            val t = HttpRequest.get(url).retry(3, 400)
                .execute().asString()
            val jsonObject = JsonParser.parseString(t) as JsonObject
            if (jsonObject["code"].asInt == 200) {
                val resp = jsonObject["message"].asJsonArray
                if (resp != null) {
                    val res = decodeJsonArray(resp)
                    RECOMMEND.addAll(res)
                    imageSearchHelper.saveMore()
                    logger.info("Add recommend size: {}", RECOMMEND.size)
                } else {
                    pixivLogin()
                    logger.warn("RECOMMEND FAILED!")
                }
            } else {
                logger.warn("RECOMMEND FAILED!: {}", t)
            }
        } catch (e: Exception) {
            pixivLogin()
            logger.error(e)
        }
    }

    /**
     * 从pixiv得到的推荐图片信息。
     * @param pureMode 健全模式。
     */
    fun recommend(pureMode: Boolean = false): PixivImage {
        if (RECOMMEND.isEmpty()) addRecommend()
        if (RECOMMEND.isEmpty()) return PixivImage.API_ERROR
        else {
            if (pureMode) {
                var pure = RECOMMEND.filter { !it.r18 }
                if (pure.isEmpty()) {
                    addRecommend()
                    pure = RECOMMEND.filter { !it.r18 }
                }

                if (pure.isEmpty()) return PixivImage.NO_PURE_IMAGE

                val res = pure.first()
                RECOMMEND.remove(res)
                return res
            }
            return RECOMMEND.removeAt(0)
        }
    }

    /**
     * 从pixiv搜索图片。
     * @param tag 关键词，使用空格隔开多个关键词。
     * @param num 大于0时，关键词添加"[num] users"指出收藏用户数以筛选掉低质量图片。
     * @return 指定关键词搜索结果列表，出错时返回空列表。
     * @throws [SendImageException] api故障
     */
    @Throws(Exception::class)
    fun searchSeTu(tag: String, num: Int): List<PixivImage> {
        if (tag.trim().isBlank()) {
            return emptyList()
        }
        val word = tag + if (num > 0) " " + num + "users" else ""
        val url = "${getPixivInfoApi()}search?limit=${Settings.getString("pixiv.searchNumber")}&word=$word"
        logger.info("Getting {}", url)
        val t = HttpRequest.get(url)
            .retry(3, 1000)
            .execute().asString()

        val res = JsonParser.parseString(t) as JsonObject
        if (res["code"].asInt == 200) {
            val jsonArray = res["message"].asJsonArray
            return decodeJsonArray(jsonArray)
        } else {
            logger.warn("GET {} FAILED! RESPONSE: {}", url, t)
            // pixivLogin()
            throw SendImageException()
        }

    }

    /**
     * 按作者uid搜索，会使用数据库中的搜索记录。
     * @param groupID 发起搜索的群号，以得到搜索历史。
     * @param uid 搜索的作者uid。
     * @return 若uid有效，返回该作者创作的随机图片，会筛选掉此群发送过的图片。
     */
    suspend fun searchByArtist(groupID: Long, uid: Long): PixivImage {
        val searchHistory =
            botDBHelper.getSomething(SearchHistory::class.java, SearchHistoryKey(groupID, uid.toString().toMd5()))
                ?: return getFromList(
                    ::searchByArtistPixiv,
                    arrayOf(uid, true),
                    uid.toString(),
                    groupID,
                    false
                )
        val omit = searchHistory.toList()
        val image = botDBHelper.searchByArtist(uid, omit = omit)
        if (image.isEmpty()) {
            return if (omit.isNotEmpty())
                getFromList(::searchByArtistPixiv, arrayOf(uid, true), uid.toString(), groupID, false)
            else
                PixivImage.NO_ARTIST_ILLUST
        }

        val res = image.random()
        searchHistory.history += "${res.pid},"
        GlobalScope.launch {
            botDBHelper.saveOrUpdateSomething(searchHistory)
        }
        return res
    }

    /**
     * 从pixiv搜索特定作者作品。
     * @param uid 搜索的作者uid。
     * @param saveToDB 是否将搜索结果保存到数据库。
     * @return 若uid有效，返回该作者创作的图片列表。
     */
    fun searchByArtistPixiv(uid: Long, saveToDB: Boolean = true): MutableList<PixivImage> {
        val url = "${getPixivInfoApi()}artist?limit=${Settings.getString("pixiv.searchNumber")}&uid=$uid"
        logger.info("Getting {}", url)
        val t = HttpRequest.get(url)
            .retry(3, 1000)
            .execute().asString()
        val res = JsonParser.parseString(t) as JsonObject
        return if (res["code"].asInt == 200) {
            val jsonArray = res["message"].asJsonArray
            val ret = decodeJsonArray(jsonArray)
            if (saveToDB) {
                GlobalScope.launch { botDBHelper.savePixivImages(ret) }
            }
            ret
        } else {
            logger.warn("GET {} FAILED! RESPONSE: {}", url, t)
            pixivLogin()
            mutableListOf()
        }
    }

    /**
     * 从pixiv搜索图片，使用 [searchSeTu] 方法搜索 [U_USERS] 中各个用户数，并自动补充可能的搜索结果。
     * @param tag 关键词，使用空格隔开多个关键词。
     * @param trans 关键词的翻译，见 [imageSearchHelper
     *.getTrans]。
     * @return 指定关键词搜索结果列表。
     */
    suspend fun searchFromPixiv(tag: String, trans: String): MutableList<PixivImage> {
        val set: MutableSet<PixivImage> = CopyOnWriteArraySet()
        coroutineScope {
            val jobs = mutableListOf<Job>()
            withTimeout(20000L) {
                U_USERS.forEach {
                    jobs.add(
                        launch {
                            set.addAll(searchSeTu(trans, it))
                        })
                    delay(300)
                }
            }
            jobs.forEach { it.join() }
            if (set.size < 15) {
                imageSearchHelper.autoComplete(tag).forEach {
                    launch {
                        set.addAll(searchSeTu(it, 1000))
                    }
                    delay(100)
                }
            }
        }
        GlobalScope.launch {
            botDBHelper.savePixivImages(set)
        }
        logger.info("Search: {}, trans: {}, result size: {}", tag, trans, set.size)
        return set.toMutableList()
    }

    /**
     * 从图片列表中过滤后取随机图片，并处理各种异常。
     */
    private suspend fun getFromList(
        function: KFunction<MutableList<PixivImage>>,
        params: Array<Any>,
        tag: String,
        groupID: Long,
        removePlural: Boolean = Settings.getBoolean("pixiv.removePlural"),
        pureMode: Boolean = false
    ): PixivImage {
        try {
            val list = if (function.isSuspend) {
                function.callSuspend(*params)
            } else {
                function.call(*params)
            }
            imageSearchHelper.saveMore()

            if (Settings.getBoolean("pixiv.illustOnly")) {
                list.removeIf { it.type != "illust" && it.type != "" }
            }

            if (list.isEmpty()) {
                return PixivImage.NO_SEARCH_RESULT
            }
            if (removePlural) {
                list.removeIf { it.p > 1 }
            }
            if (list.isEmpty()) return PixivImage.NO_SINGLE_IMAGE

            if (pureMode) {
                list.removeIf { it.r18 }
            }
            if (list.isEmpty()) return PixivImage.NO_PURE_IMAGE

            val res = list.random()

            if (res.pid > 0) {
                GlobalScope.launch {
                    botDBHelper.saveOrUpdateSomething(SearchHistory(groupID, tag, "${res.pid},"))
                }
            }
            return res
        } catch (e: JsonSyntaxException) {
            pixivLogin()
            return PixivImage.API_ERROR
        } catch (e: JsonParseException) {
            pixivLogin()
            return PixivImage.API_ERROR
        } catch (e: SendImageException) {
            return PixivImage.API_ERROR
        } catch (e: SocketTimeoutException) {
            logger.error(e)
            return PixivImage.TIME_OUT
        } catch (e: TimeoutCancellationException) {
            logger.warn("${function.name} timeout cancelled")
            return PixivImage.TIME_OUT
        } catch (e: Exception) {
            logger.error(e)
            return PixivImage.UNKNOWN_ERROR
        }
    }

    /**
     * 使用pixiv关键词搜索图片，并进行各种过滤后得到一张随机图片。
     * @param groupID 发起搜索的群号，以得到搜索历史。
     * @param tag 关键词，使用空格隔开多个。
     * @param trans 关键词翻译，见 [imageSearchHelper
     *.getTrans]。
     * @param removePlural 去除 [图片数][PixivImage.p] > 1 的图片。
     * @param forceSearchFromPixiv 强制使用pixiv搜索而不通过数据库。
     * @param pureMode 健全模式。
     * @return 过滤后结果中的随机图片，或异常信息（pid < 0 的 [PixivImage]）
     */
    suspend fun getSetuInfo(
        groupID: Long,
        tag: String,
        trans: String,
        removePlural: Boolean = Settings.getBoolean("pixiv.removePlural"),
        forceSearchFromPixiv: Boolean = false,
        pureMode: Boolean = false
    ): PixivImage {
        if (forceSearchFromPixiv) {
            return getFromList(
                ::searchFromPixiv, arrayOf(tag, trans),
                trans,
                groupID,
                removePlural,
                pureMode
            )
        }
        val searchHistory =
            botDBHelper.getSomething(SearchHistory::class.java, SearchHistoryKey(groupID, trans.toMd5()))
                ?: return if (botDBHelper.hasHistory(trans)) {
                    getFromList(
                        botDBHelper::searchPixivImage,
                        arrayOf(
                            trans,
                            removePlural,
                            Settings.getInt("pixiv.db.searchNumber"),
                            emptyList<Long>(),
                            pureMode,
                            Settings.getBoolean("pixiv.illustOnly")
                        ),
                        trans,
                        groupID,
                        removePlural
                    )
                } else {
                    getFromList(
                        ::searchFromPixiv, arrayOf(tag, trans),
                        trans,
                        groupID,
                        removePlural,
                        pureMode
                    )
                }

        val omit = searchHistory.toList()
        val image = botDBHelper.searchPixivImage(trans, removePlural, omit = omit, pureMode = pureMode)
        if (image.isEmpty() && omit.isNotEmpty()) {
            return getFromList(::searchFromPixiv, arrayOf(tag, trans), trans, groupID, removePlural, pureMode)
        }

        val res = image.random()
        searchHistory.history += "${res.pid},"
        GlobalScope.launch {
            botDBHelper.saveOrUpdateSomething(searchHistory)
        }
        return res
    }

    /**
     * 得到指定pid的图片信息。
     * @param pid 图片pid。
     * @return 图片信息，或错误信息。
     */
    fun getIllust(pid: Long): PixivImage {
        if (pid < 0) return PixivImage(pid)
        val db = botDBHelper.getSomething(PixivImage::class.java, pid)
        if (db != null)
            return db
        val url = "${getPixivInfoApi()}illust?pid=$pid"
        logger.debug("Getting {}", url)
        return try {
            val t = HttpRequest.get(url).retry(3, 400)
                .execute().asString()
            val res = JsonParser.parseString(t) as JsonObject
            when (res["code"].asInt) {
                200 -> {
                    val illust = res["message"].asJsonObject
                    val img = decodeImgJSON(illust)
                    botDBHelper.savePixivImages(listOf(img))
                    img
                }
                -2 -> {
                    PixivImage.NO_SUCH_ILLUST
                }
                else -> {
                    //pixivLogin()
                    PixivImage.API_ERROR
                }
            }
        } catch (e: JsonParseException) {
            logger.error(e)
            PixivImage.API_ERROR
        } catch (e: Exception) {
            logger.error(e)
            PixivImage.UNKNOWN_ERROR
        }
    }

    private fun decodeJsonArray(array: JsonArray) = array.map { decodeImgJSON(it.asJsonObject) }.toMutableList()

    /**
     * 处理json信息，并将翻译添加到翻译库。
     */
    private fun decodeImgJSON(imageInfo: JsonObject): PixivImage {
        var urls =
            if (Settings.getBoolean("pixiv.sendLargeImage")) imageInfo["large_url"].asString else imageInfo["medium_url"].asString
        var urlLarge = imageInfo["original_url"].asString
        urlLarge = urlLarge.replace("i\\.pximg\\.net".toRegex(), "i.pixiv.cat")
        urls = urls.replace("i\\.pximg\\.net".toRegex(), "i.pixiv.cat")

        val tags = imageInfo["tags"].asJsonArray

        val builder = StringBuilder()
        for (tag in tags) {
            val name = tag.asJsonObject["name"]
            val tName = tag.asJsonObject["translated_name"]
            var nameS: String? = null
            var tNameS: String? = null
            if (!name.isJsonNull) {
                nameS = name.asString.replace(",", " ")
            }
            if (!tName.isJsonNull) {
                tNameS = tName.asString.replace(",", " ")
            }
            if (nameS != null && tNameS != null) {
                imageSearchHelper.addTrans(tNameS, nameS)
            }

            nameS?.let { imageSearchHelper.getLocalTrans(it) }?.let { tNameS = it }

            builder.append("${nameS ?: ""},${tNameS ?: ""},")
        }

        val res = PixivImage(
            imageInfo["id"].asLong,
            imageInfo["page_count"].asInt,
            imageInfo["user_id"].asLong,
            imageInfo["title"].asString,
            imageInfo["user_name"].asString,
            urls,
            urlLarge,
            imageInfo["r18"].asBoolean,
            builder.toString()
        )
        res.type = imageInfo["type"].asString
        return res
    }

    @Scheduled(initialDelay = 1500_000, fixedRate = 1500_000)
    fun autoLogin() {
        pixivLogin()
    }
}