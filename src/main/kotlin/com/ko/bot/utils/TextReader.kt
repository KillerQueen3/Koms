package com.ko.bot.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.toMessageChain
import java.io.*
import java.lang.Exception
import java.lang.NullPointerException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap

/**
 * 读取message.json中的文本的工具类，用于自定义文本。
 */
object TextReader {
    private val texts: MutableMap<String, String?> = HashMap()

    /**
     * 将文件中的信息读取到内存中。
     */
    @JvmStatic
    fun loadTexts() {
        val file = File("./resource/message.json")
        if (!file.exists()) {
            throw FileNotFoundException("resource/message.json 文件不存在！")
        }
        try {
            BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use {
                val res = Gson().fromJson<Map<String, String?>>(it, object : TypeToken<Map<String?, String?>?>() {}.type)
                    ?: throw NullPointerException("resource/message.json 空文件！")
                texts.clear()
                texts.putAll(res)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getString(key: String): String {
        return texts[key]?:""
    }

    @JvmStatic
    fun getText(key: String, sender: Member?): MessageChain {
        val res = texts.getOrDefault(key, key)
        return if (res!!.contains("[at]") && sender != null) {
            At(sender).plus(res.replace("\\[at]".toRegex(), ""))
        } else PlainText(res).toMessageChain()
    }

    @JvmStatic
    fun getText(key: String): MessageChain {
        return PlainText(texts.getOrDefault(key, key)!!).toMessageChain()
    }

    /**
     * 得到 [key] 指明的文本由 [split] 分割后列表中的随机文本，将 [sender] 注入at消息。
     * @param key 文本键值
     * @param split 分隔符
     * @param sender at对象
     * @return message.json中指定文本中随机一项，注入at信息。
     */
    @JvmStatic
    fun getRandomText(key: String, split: String, sender: Member): MessageChain {
        val res = texts.getOrDefault(key, null) ?: return PlainText(key).toMessageChain()
        val ran = res.split(split.toRegex()).toTypedArray().random()

        return if (ran.contains("[at]")) {
            At(sender.id).plus(ran.replace("\\[at]".toRegex(), ""))
        } else PlainText(ran).toMessageChain()
    }

    /**
     * 得到 [key] 指明的文本由 [split] 分割后列表中的随机文本。
     * @param key 文本键值
     * @param split 分隔符
     * @return message.json中指定文本中随机一项。
     */
    @JvmStatic
    fun getRandomText(key: String, split: String): PlainText {
        val res = texts.getOrDefault(key, null) ?: return PlainText(key)
        return PlainText(res.split(split.toRegex()).toTypedArray().random())
    }

    /**
     * 得到 [key] 指明的文本由 [split] 分割后列表中的文本列表。
     * @param key 文本键值
     * @param split 分隔符
     * @return message.json中指定文本分割后的列表。
     */
    @JvmStatic
    fun getStrings(key: String, split: String): List<String> {
        val r = texts.getOrDefault(key, null) ?: return ArrayList()
        val sp = r.split(split.toRegex()).toTypedArray()
        return listOf(*sp)
    }
}