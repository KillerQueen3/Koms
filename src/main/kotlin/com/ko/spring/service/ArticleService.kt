package com.ko.spring.service

import com.ko.spring.dao.impl.KomsSpringDao
import com.ko.spring.domin.Article
import com.ko.spring.domin.SimpleArticle
import com.ko.spring.exception.ArticleException
import com.ko.spring.utils.getUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ArticleService @Autowired constructor(private val dao: KomsSpringDao) {
    fun getArticle(id: String): Article? {
        return dao.getById(Article::class.java, id)
    }

    fun newArticle(article: SimpleArticle):String {
        val user = getUser()?: throw ArticleException("无权创建！")
        return dao.createArticle(user, article.html, article.title, article.tags)
    }

    fun editArticle(article: SimpleArticle) {
        val user = getUser()?: throw ArticleException("无权修改！")
        dao.editArticle(user, article)
    }

    fun hasAuth(article: Article): Boolean {
        val user = getUser()?: return false
        if (user.hasAuth("AUTH_SUPER_ADMIN")) return true
        if (article.authorID == user.id) return true
        return false
    }

    fun deleteArticle(id : String) {
        val article = getArticle(id)?: throw ArticleException("不存在的文章！")
        if (!hasAuth(article)) throw ArticleException("无权删除！")
        dao.deleteArticle(id)
    }
}