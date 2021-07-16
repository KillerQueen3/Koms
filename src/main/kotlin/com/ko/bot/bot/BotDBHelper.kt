package com.ko.bot.bot

import com.ko.bot.entity.*
import com.ko.bot.utils.Settings
import com.ko.spring.utils.toMd5
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable


/**
 * 机器人数据库IO工具类，使用Hibernate框架。
 */
@Component
class BotDBHelper  {
    @Autowired private lateinit var factory: SessionFactory

    /**
     * 存储或更新一个对象。
     *
     * **注意：[thing] 的类必须在Hibernate配置文件中声明！**
     */
    fun saveOrUpdateSomething(thing: Any) {
        val session = factory.currentSession
        session.beginTransaction()
        session.saveOrUpdate(thing)
        session.transaction.commit()
    }

    /**
     * 删除一个对象。
     */
    fun deleteSomething(thing: Any) {
        val session = factory.currentSession
        session.beginTransaction()
        session.delete(thing)
        session.transaction.commit()
    }

    /**
     * 得到一个指定 [id] 的 [clazz] 对象。
     */
    fun <T> getSomething(clazz: Class<T>, id: Serializable): T? {
        val session = factory.currentSession
        session.beginTransaction()
        val res = session.get(clazz, id)
        session.transaction.commit()
        return res
    }

    /**
     * 执行一个sql更新语句。
     * @param sql 要执行的sql语句，update或delete。
     */
    fun sqlUpdate(sql: String) {
        val session = factory.currentSession
        session.beginTransaction()
        session.createQuery(sql).executeUpdate()
        session.transaction.commit()
    }

    /**
     * 执行一个hql查询语句
     * @param hql 要执行的hql语句
     * @param clazz 返回结果类
     * @param num 最大返回数量
     * @return 查询结果
     */
    fun <T> sqlQuery(hql: String, clazz: Class<T>, num: Int = -1): MutableList<T> {
        val session = factory.currentSession
        session.beginTransaction()
        val query = session.createQuery(hql, clazz)
        if (num > 0)
            query.maxResults = num
        val res = query.list()
        session.transaction.commit()
        return res
    }

    fun savePixivImages(images: Collection<PixivImage>) {
        val session = factory.currentSession
        session.beginTransaction()
        for (image in images)
            if (image.pid > 0)
                session.saveOrUpdate(image)
        try {
            session.transaction.commit()
        } catch (e: Exception) {

        }
    }

    /**
     * 在数据库中搜索 [PixivImage]。
     * @param keyword 关键词。
     * @param num 最大搜索数。
     * @param single 为 true 时指定只搜索单张图片（[PixivImage.p] == 1）。
     * @param omit 忽略的 pid 列表。
     * @param pureMode 为 true 时不搜索r18图片，默认为false。
     * @param illustOnly 仅搜索type为illust的图片
     * @return 搜索结果列表。
     */
    fun searchPixivImage(keyword: String, single: Boolean, num: Int = Settings.getInt("pixiv.db.searchNumber"), omit: Collection<Long> = emptyList(), pureMode: Boolean = false, illustOnly: Boolean = Settings.getBoolean("pixiv.illustOnly")): MutableList<PixivImage> {
        if (num < 1)
            throw Exception("num不可小于1")
        val session = factory.currentSession
        session.beginTransaction()
        val res = mutableListOf<PixivImage>()

        val ks = keyword.split(" ")
        val builder = StringBuilder("FROM PixivImage WHERE ")
        var and = false
        for (k in ks) {
            if (and)
                builder.append("AND ")
            builder.append("concat(' ', title, tagStr) LIKE '%$k%' ")
            and = true
        }

        if (single)
            builder.append("AND p = 1 ")

        if (omit.isNotEmpty()) {
            builder.append("AND pid NOT IN $omit ".replace("[", "(").replace("]", ")"))
        }

        if (pureMode) {
            builder.append("AND r18 = false ")
        }

        if (illustOnly) {
            builder.append("AND type = 'illust'")
        }

        val query = session.createQuery(
            "$builder ORDER BY pid DESC",
            PixivImage::class.java)

        query.maxResults = num
        res.addAll(query.list())
        session.transaction.commit()
        return res
    }

    /**
     * 在数据库中搜索指定作者的作品。
     * @param uid 作者uid
     * @param num 最大返回数量
     * @param omit 排除的pid列表
     * @return 结果列表。
     */
    fun searchByArtist(uid: Long, num: Int = Settings.getInt("pixiv.db.searchNumber"), omit: Collection<Long> = emptyList()): MutableList<PixivImage> {
        val session = factory.currentSession
        val res = mutableListOf<PixivImage>()
        session.beginTransaction()

        val builder = StringBuilder("FROM PixivImage WHERE uid=${uid} ")


        if (omit.isNotEmpty()) {
            builder.append("AND pid NOT IN $omit ".replace("[", "(").replace("]", ")"))
        }

        val query = session.createQuery(
            "$builder ORDER BY pid DESC",
            PixivImage::class.java)

        query.maxResults = num
        res.addAll(query.list())
        session.transaction.commit()
        return res
    }


    /**
     * 返回是否存在指定标签的搜索记录。
     * @param tag 要搜索的标签
     * @return 是否存在搜索记录
     */
    fun hasHistory(tag: String): Boolean {
        val session = factory.currentSession
        session.beginTransaction()
        val res = session.createQuery("FROM SearchHistory WHERE tagMd5 = '${tag.toMd5()}'", SearchHistory::class.java)
        val b = res.list().size > 0
        session.transaction.commit()
        return b
    }

    /**
     * 保存翻译信息。
     */
    fun saveTrans(trans: Map<String, String>) {
        val session = factory.currentSession
        session.beginTransaction()
        for ((k, v) in trans) {
            session.saveOrUpdate(Trans(k, v))
        }
        try {
            session.transaction.commit()
        } catch (e: Exception) {

        }
    }

    fun completeTrans(tag: String): List<String> {
        val session = factory.currentSession
        session.beginTransaction()
        val query = session.createQuery("FROM Trans WHERE ch LIKE '%$tag%'", Trans::class.java)
        val ts = query.list()
        val res = mutableListOf<String>()
        for (t in ts) {
            res.add(t.jp)
        }
        session.transaction.commit()
        return res
    }

    fun getSubscribes(isActive: Boolean = true): List<SubscribeArtist> {
        val session = factory.currentSession
        session.beginTransaction()
        var hql = "FROM SubscribeArtist"
        if (isActive) {
            hql += " WHERE isActive=true"
        }
        val query = session.createQuery(hql, SubscribeArtist::class.java)
        val res = query.list()
        session.transaction.commit()
        return res
    }
}