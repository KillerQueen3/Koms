package com.ko.bot.img

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.ko.bot.entity.Trans
import com.ko.bot.bot.BotDBHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import kotlin.collections.set

/**
 * 图片搜索辅助类。
 */
@Component
class ImageSearchHelper {
    private val trans: MutableMap<String, String> = HashMap()
    private val chara: MutableMap<String, String> = HashMap()
    private val moreTrans: MutableMap<String, String> = ConcurrentHashMap()

    @Autowired private lateinit var botDBHelper: BotDBHelper

    fun getLocalTrans(keyword: String) = trans[keyword]

    /**
     * 添加额外翻译到内存。
     */
    fun addTrans(ch: String, jp: String) {
        moreTrans.putIfAbsent(ch.toLowerCase(), jp)
    }

    /**
     * 存储额外翻译，并清理内存。
     */
    fun saveMore() {
        GlobalScope.launch {
            botDBHelper.saveTrans(moreTrans)
            moreTrans.clear()
        }
    }

    private fun keywordReplace(raw: String): String { // 替换掉中日文不同的汉字。
        return raw.replace("姬".toRegex(), "姫").replace("穗", "穂")
    }

    /**
     * 得到单个tag的翻译。
     */
    private fun getSingleTrans(keyword: String): String {
        val chara = chara[keyword]
        if (chara != null) return chara
        val trans = trans[keyword]
        if (trans != null) return trans
        val db = botDBHelper.getSomething(Trans::class.java, keyword) ?: return keyword
        return db.jp
    }

    /**
     * 得到翻译，多个关键词请使用空格隔开。
     * @param keyword 关键词
     * @return 翻译，若有多个关键词，使用空格隔开。
     */
    fun getTrans(keyword: String): String {
        var kw = keyword
        val r18 = keyword.matches(".*[rR]-?18.*".toRegex())
        if (r18)
            kw = keyword.replace("[rR]-?18".toRegex(), "")
        if (kw.isEmpty() && r18)
            return "R-18"
        val keywords = kw.toLowerCase().split(" ".toRegex()).toTypedArray()
        for (i in keywords.indices) {
            if (keywords[i].isNotEmpty())
                keywords[i] = getSingleTrans(keywords[i])
        }
        return keywordReplace(StringUtils.join(keywords, " ") + if (r18) " R-18" else "")
            .replace("  ", " ").trim()
    }

    /**
     * 自动填充翻译。
     */
    fun autoComplete(src: String): Set<String> {
        val res: MutableSet<String> = HashSet()
        for ((k, v) in trans) {
            if (k != src && k.contains(src)) {
                res.add(v)
            }
        }
        res.addAll(botDBHelper.completeTrans(src))
        return res
    }

    @PostConstruct
    fun reload() {
        trans.clear()
        trans.putAll(getTrans())
        chara.clear()
        chara.putAll(getChara())
    }

    private fun getTrans(): Map<String, String> {
        try {
            val reader = FileReader("./resource/trans.json")
            val read =
                Gson().fromJson<Map<String, String>>(reader, object : TypeToken<Map<String?, String?>?>() {}.type)
            if (read != null) {
                return read
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return HashMap()
    }

    private fun getChara(): Map<String, String> {
        val res: MutableMap<String, String> = HashMap()
        try {
            val file = File("./resource/pcrChara.json")
            val br = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
            val charas: JsonArray = JsonParser.parseReader(br).asJsonArray
            for (c in charas) {
                val ob = c.asJsonObject
                val jpName: String = ob["jp"].asString + " プリコネ"
                res[ob["ch"].asString] = jpName
                res[jpName] = jpName
                for (n in ob["nick"].asJsonArray) {
                    res.putIfAbsent(n.asString.toLowerCase(), jpName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }
}