package com.ko.spring.controller

import com.ko.bot.entity.PixivImage
import com.ko.bot.img.ImageSearch
import com.ko.bot.img.ImageSearchHelper
import com.ko.spring.utils.getUser
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping("/pixiv")
class PixivController {
    @Autowired private lateinit var imageSearch: ImageSearch
    @Autowired private lateinit var imageSearchHelper: ImageSearchHelper

    @GetMapping(value = ["", "/index"])
    fun index(): ModelAndView {
        val mv = ModelAndView("pixiv")
        val user = getUser() ?: return mv
        mv.addObject("user", user)
        return mv
    }

    @GetMapping("/random")
    @ResponseBody
    fun random(): PixivImage {
        return if (getUser() == null) imageSearch.getSetuInfo(true)
        else imageSearch.getSetuInfo()
    }

    @GetMapping("/pid")
    @ResponseBody
    fun getByPid(@RequestParam pid: Long):PixivImage {
        return imageSearch.getIllust(pid)
    }

    @GetMapping("/artist")
    @ResponseBody
    fun artist(@RequestParam uid: Long,
                       httpServletRequest: HttpServletRequest): PixivImage {
        var res : PixivImage
        runBlocking {
            res = imageSearch.searchByArtist(getIPBasedID(httpServletRequest), uid)
        }
        return res
    }

    @GetMapping("/search")
    @ResponseBody
    fun search(@RequestParam keyword: String,
                           @RequestParam r18: Boolean=false,
                           httpServletRequest: HttpServletRequest): PixivImage {
        val kw = (keyword + if (r18) "R-18" else "").replace("[,，]".toRegex(), " ")
        val user = getUser()
        var res: PixivImage
        runBlocking {
            res = if (user == null) {
                imageSearch.getSetuInfo(getIPBasedID(httpServletRequest), kw, imageSearchHelper.getTrans(kw), removePlural = false, pureMode = true)
            } else {
                imageSearch.getSetuInfo(user.id.hashCode().toLong(), kw, imageSearchHelper.getTrans(kw), removePlural = false)
            }
        }

        return res
    }

    @GetMapping("/recommend")
    @ResponseBody
    fun recommend(): PixivImage {
        return imageSearch.recommend()
    }

    private fun getIPBasedID(httpServletRequest: HttpServletRequest): Long {
        val addr = "19" + httpServletRequest.remoteAddr.replace("[:.]".toRegex(), "")
        return try {
            addr.toLong()
        } catch (e: NumberFormatException) {
            114514
        }
    }
}