package com.ko.spring.dao.impl

import com.ko.spring.domin.Article
import com.ko.spring.domin.CustomUser
import com.ko.spring.domin.SimpleArticle
import com.ko.spring.exception.ArticleException
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Component
class KomsSpringDao @Autowired constructor(private val factory: SessionFactory) {
    fun sorAnyThing(thing: Any) {
        val session = factory.currentSession
        session.beginTransaction()
        session.saveOrUpdate(thing)
        session.transaction.commit()
    }

    fun <T> getById(clazz: Class<T>, id: Serializable): T? {
        val session = factory.currentSession
        session.beginTransaction()
        val res = session.get(clazz, id)
        session.transaction.commit()
        return res
    }

    fun createArticle(customUser: CustomUser, html: String, title: String, tags: String): String {
        val session = factory.currentSession
        session.beginTransaction()
        val df = SimpleDateFormat("yyyyMMdd")
        val ds = df.format(Date())
        val query = session.createQuery("FROM Article WHERE id LIKE '$ds%'", Article::class.java)
        val list = query.list()
        var id = "000"
        if (list.size >= 1000) {
            throw ArticleException("今日文章达到上限！")
        }
        if (list.size > 0) {
            id = String.format("%03d", list.size)
        }
        session.clear()
        session.save(Article(ds + id,  Date(), customUser.name, customUser.id, html, title, tags))
        session.transaction.commit()
        return ds + id
    }

    fun editArticle(customUser: CustomUser, simpleArticle: SimpleArticle) {
        val session = factory.openSession()
        session.beginTransaction()
        val article = session.get(Article::class.java, simpleArticle.id) ?: throw ArticleException("指定文章不存在！")
        if (article.authorID != customUser.id) {
            if (!customUser.hasAuth("AUTH_SUPER_ADMIN")) {
                throw ArticleException("无权修改！")
            }
        }
        val newA = article.copy(title = simpleArticle.title, html = simpleArticle.html, tagS = simpleArticle.tags, lastEditTime = Date())
        session.clear()
        session.update(newA)
        session.transaction.commit()
        session.close()
    }

    fun deleteArticle(id: String) {
        val session = factory.currentSession
        session.beginTransaction()
        session.createQuery("UPDATE Article SET deleted = true WHERE id = '$id'").executeUpdate()
        session.transaction.commit()
    }
}