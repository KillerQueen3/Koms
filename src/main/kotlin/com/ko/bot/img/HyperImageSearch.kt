package com.ko.bot.img

import com.ko.bot.entity.PixivImage
import com.ko.bot.entity.SearchHistory
import com.ko.bot.entity.SubscribeArtist
import com.ko.bot.entity.TagMapping
import com.ko.bot.bot.BotDBHelper
import com.ko.spring.utils.toTagList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * 图片搜索高级功能工具类。
 */
@Component
class HyperImageSearch {
    /**
     * 存储图片映射，请保持与数据库同步。
     */
    val mappings: MutableList<TagMapping> = mutableListOf()
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var imageSearchHelper: ImageSearchHelper

    @Autowired
    private lateinit var imageSearch: ImageSearch

    @Autowired
    private lateinit var botDBHelper: BotDBHelper

    @PostConstruct
    fun init() {
        mappings.addAll(botDBHelper.sqlQuery("FROM TagMapping", TagMapping::class.java))
    }

    /**
     * 检查 [tag] 能搜索到的图片数量。
     */
    private suspend fun checkTag(tag: String): Int {
        val trans = imageSearchHelper.getTrans(tag)
        if (botDBHelper.hasHistory(trans)) {
            return botDBHelper.searchPixivImage(trans, false, 500).size
        }

        val list = imageSearch.searchFromPixiv(tag, trans)
        if (list.size > 0) {
            GlobalScope.launch {
                botDBHelper.saveOrUpdateSomething(SearchHistory(42, trans))
            }
        }
        return list.size
    }

    /**
     * 得到 [thing] 锁映射到的tag。
     * @param thing 输入值
     * @return [thing] 符合的 [TagMapping] 的 [标签集][TagMapping.tags] 中的随机一个标签。没有符合时返回 null。
     */
    @Nullable
    fun getMappedTag(thing: String): String? {
        for (mapping in mappings) {
            if (thing.matches(mapping.regex.toRegex())) {
                return mapping.tags.toTagList().random()
            }
        }
        return null
    }

    /**
     * 检查一组标签。
     * @param maps 要检查的标签组，由","隔开
     * @return 结果信息（json），供前端使用。
     */
    suspend fun checkTagMapping(maps: String): JsonArray {
        val list = maps.toTagList()
        return buildJsonArray {
            for (m in list) {
                add(
                    buildJsonObject {
                        put("tag", m)
                        put("trans", imageSearchHelper.getTrans(m))
                        put("num", checkTag(m))
                    })
            }
        }
    }

    private suspend fun filerTag(tags: List<String>, list: MutableList<String>, id: Int): String {
        val resMapping = StringBuilder()
        val exist = mappings.filter { it.id == id }
        val existTags = mutableListOf<String>()
        if (exist.isNotEmpty()) {
            existTags.addAll(exist[0].tags.toTagList())
        }
        for (t in tags) {
            if (existTags.contains(t)) {
                resMapping.append("$t,")
            } else {
                if (checkTag(t) > 0) {
                    resMapping.append("$t,")
                } else {
                    list.add(t)
                }
            }
        }
        return resMapping.toString().substring(0, resMapping.length - 1)
    }

    suspend fun editTagMapping(id: Int, newEntry: String, newMapping: String): List<String>? {
        val list = newMapping.toTagList()
        if (list.isEmpty() || newEntry.isEmpty()) {
            return null
        }

        val emptyTags = mutableListOf<String>()
        val tagMapping = TagMapping(id, newEntry, filerTag(list, emptyTags, id))
        mappings.removeIf { it.id == tagMapping.id }
        mappings.add(tagMapping)
        botDBHelper.saveOrUpdateSomething(tagMapping)
        return emptyTags
    }

    suspend fun addTagMapping(regex: String, mapping: String): List<String>? {
        val list = mapping.toTagList()
        if (list.isEmpty() || regex.isEmpty()) {
            return null
        }
        val emptyTags = mutableListOf<String>()
        val tagMapping = TagMapping(regex, filerTag(list, emptyTags, 0))
        mappings.add(tagMapping)
        botDBHelper.saveOrUpdateSomething(tagMapping)
        return emptyTags
    }

    fun deleteTagMapping(id: Int) {
        mappings.removeIf { it.id == id }
        botDBHelper.deleteSomething(TagMapping(id))
    }

    fun getArtistName(uid: Long) : String {
        val db = botDBHelper.searchByArtist(uid, 1)
        if (db.isNotEmpty()) {
            return db[0].author?:"未知"
        }
        val pixiv = imageSearch.searchByArtistPixiv(uid, false)
        if (pixiv.isNotEmpty()) {
            return pixiv[0].author?:"未知"
        }
        return "错误"
    }

    data class ArtistInfo(val uid: Long, val name: String, val num: Int)

    fun checkArtist(artistUid: Long): ArtistInfo? {
        val res = botDBHelper.searchByArtist(artistUid, 1)
        if (res.isEmpty()) {
            res.addAll(imageSearch.searchByArtistPixiv(artistUid, false))
        }
        if (res.isEmpty()) return null
        val p = res[0]
        if (p.pid < 0) return null
        return p.author?.let { ArtistInfo(p.uid, it, res.size) }
    }

    fun subscribe(groupID: Long, artistUid: Long, disable: Boolean = false): Int {
        if (!disable) {
            botDBHelper.saveOrUpdateSomething(SubscribeArtist(groupID, artistUid))
            logger.info("{} subscribed {}", groupID, artistUid)
        } else {
            botDBHelper.saveOrUpdateSomething(SubscribeArtist(groupID, artistUid))
            logger.info("{} unsubscribed {}", groupID, artistUid)
            return 0
        }
        return imageSearch.searchByArtistPixiv(artistUid).size
    }

    private fun getNew(artistUid: Long): List<PixivImage> {
        val searched = imageSearch.searchByArtistPixiv(artistUid, false)
        val res = mutableListOf<PixivImage>()
        for (p in searched) {
            if (botDBHelper.getSomething(PixivImage::class.java, p.pid) == null && p.pid > 0) {
                res.add(p)
            }
        }
        return res
    }

    /**
     * 准备订阅信息。
     */
    fun prepareSubscribe(): Map<Long, List<PixivImage>> {
        val subscribes = botDBHelper.getSubscribes()
        val prepared = mutableMapOf<Long, MutableList<PixivImage>>()
        val searchedArtist = mutableMapOf<Long, List<PixivImage>>()
        for (s in subscribes) {
            val news = mutableListOf<PixivImage>()
            if (!searchedArtist.contains(s.subscribeKey.artistUid)) {
                news.addAll(getNew(s.subscribeKey.artistUid))
                logger.info("Subscribe: uid = {}, new = {}", s.subscribeKey.artistUid, news.size)
                if (news.size > 7) {
                    logger.warn("Too many new Images! uid={}", s.subscribeKey.artistUid)
                    botDBHelper.savePixivImages(news)
                    searchedArtist[s.subscribeKey.artistUid] = emptyList()
                    continue
                }
                botDBHelper.savePixivImages(news)
                searchedArtist[s.subscribeKey.artistUid] = news
            } else {
                searchedArtist[s.subscribeKey.artistUid]?.let { news.addAll(it) }
            }

            if (news.size > 0) {
                if (prepared[s.subscribeKey.groupID] == null) {
                    prepared[s.subscribeKey.groupID] = news
                } else {
                    prepared[s.subscribeKey.groupID]?.addAll(news)
                }
            }
        }
        return prepared
    }
}
