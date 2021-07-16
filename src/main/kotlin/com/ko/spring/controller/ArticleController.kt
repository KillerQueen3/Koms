package com.ko.spring.controller

import com.ko.spring.domin.Response
import com.ko.spring.domin.SimpleArticle
import com.ko.spring.service.ArticleService
import com.ko.spring.utils.getUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Controller
@RequestMapping("/article")
class ArticleController @Autowired constructor(private val articleService: ArticleService) {
    @GetMapping("/read/{id}")
    fun readArticle(@PathVariable id: String): ModelAndView {
        val article = articleService.getArticle(id) ?: return ModelAndView("error/articleNotFound")
        if (article.deleted) return ModelAndView("error/articleNotFound")
        val mov = ModelAndView("article")
        mov.addObject("user", getUser())
        mov.addObject("article", article)
        mov.addObject("hasAuth", articleService.hasAuth(article))
        return mov
    }

    @GetMapping("/edit/{id}")
    fun editArticle(@PathVariable id: String): ModelAndView {
        val article = articleService.getArticle(id) ?: return ModelAndView("error/articleNotFound")
        if (!articleService.hasAuth(article)) return ModelAndView("error/articleNotFound")
        val mov = ModelAndView("articleEdit")
        mov.addObject("user", getUser())
        mov.addObject(article)
        mov.addObject("edit", true)
        return mov
    }

    @GetMapping("/new")
    fun newArticle(): ModelAndView {
        val mov = ModelAndView("articleEdit")
        mov.addObject("user", getUser())
        mov.addObject("edit", false)
        return mov
    }

    @PostMapping("/edit/edit")
    @ResponseBody
    fun editArticle(@RequestBody body: SimpleArticle): Response {
        articleService.editArticle(body)
        return Response("完成！")
    }

    @PostMapping("/edit/create")
    @ResponseBody
    fun createArticle(@RequestBody body: SimpleArticle): Response {
        return Response(articleService.newArticle(body))
    }

    @PostMapping("/edit/uploadImage", consumes = ["multipart/form-data"])
    @ResponseBody
    fun upload(@RequestParam file: MultipartFile): Response {
        val ds = SimpleDateFormat("yyyyMMdd").format(Date())
        var localFile = File("./image/$ds-${UUID.randomUUID()}.jpg")
        while (localFile.exists()) {
            localFile = File("./image/$ds-${UUID.randomUUID()}.jpg")
        }
        return try {
            file.transferTo(localFile)
            Response("/image/${localFile.name}")
        } catch (e: Exception) {
            Response(500, "错误！")
        }
    }

    @PostMapping("/edit/delete")
    @ResponseBody
    fun delete(@RequestParam id: String): Response {
        articleService.deleteArticle(id)
        return Response("成功！")
    }
}