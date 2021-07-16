package com.ko.spring.controller

import com.ko.bot.bot.MyBot
import com.ko.bot.img.HyperImageSearch
import com.ko.spring.domin.Response
import com.ko.spring.service.PixivArtistSubscribeService
import com.ko.spring.utils.getUser
import kotlinx.serialization.json.JsonArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Controller
@RequestMapping("/setu")
class SetuAdminController @Autowired constructor(private val pixivArtistSubscribeService: PixivArtistSubscribeService) {
    @Autowired private lateinit var myBot: MyBot
    @Autowired private lateinit var hyperImageSearch: HyperImageSearch

    @RequestMapping("")
    fun setu(): ModelAndView {
        val mv = ModelAndView("setuAdmin")
        mv.addObject("user", getUser())
        mv.addObject("tagList", hyperImageSearch.mappings)
        mv.addObject("subMap", pixivArtistSubscribeService.getSubscribes())
        mv.addObject("ready", myBot.ready)
        if (myBot.ready) {
            mv.addObject("groups", myBot.bot.groups)
        }
        return mv
    }

    @GetMapping("/check")
    @ResponseBody
    suspend fun checkTags(@RequestParam("tags") tags: String): JsonArray {
        return hyperImageSearch.checkTagMapping(tags)
    }

    @PostMapping("/edit")
    @ResponseBody
    suspend fun editMapping(
        @RequestParam("id") id: Int,
        @RequestParam("key") key: String,
        @RequestParam("mapping") mapping: String
    ): Response {
        val list = hyperImageSearch.editTagMapping(id, URLDecoder.decode(key, StandardCharsets.UTF_8), mapping)
            ?: return Response(400, "错误的输入！")
        return Response(list)
    }

    @PostMapping("/new")
    @ResponseBody
    suspend fun newMapping(
        @RequestParam("key") key: String,
        @RequestParam("mapping") mapping: String
    ): Response {
        val list = hyperImageSearch.addTagMapping(key, mapping) ?: return Response(400, "错误的输入！")
        return Response(list)
    }

    @PostMapping("/delete")
    @ResponseBody
    fun deleteMapping(@RequestParam("id") id: Int) {
        hyperImageSearch.deleteTagMapping(id)
    }

    @PostMapping("/newSub")
    @ResponseBody
    fun newSubscribe(@RequestParam("groupID") groupID: Long, @RequestParam("uid") uid: Long): Response {
        if (pixivArtistSubscribeService.addSubscribe(groupID, uid))
            return Response("成功！")
        return Response(400, "失败！")
    }

    @PostMapping("/deleteSub")
    @ResponseBody
    fun deleteSub(@RequestParam("groupID") groupID: Long, @RequestParam("uid") uid: Long): Response {
        if (pixivArtistSubscribeService.deleteSubscribe(groupID, uid, true))
            return Response("成功！")
        return Response(400, "失败！")
    }

    @GetMapping("/artistInfo")
    @ResponseBody
    fun artistInfo(@RequestParam("uid") uid: Long): Response {
        val info = hyperImageSearch.getArtistName(uid)
        return if (info == "错误" || info == "未知") {
            Response(400, "错误的uid")
        } else {
            Response(info)
        }
    }
}