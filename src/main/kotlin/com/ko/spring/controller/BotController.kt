package com.ko.spring.controller

import com.ko.bot.bot.MyBot
import com.ko.bot.plugin.PluginManager
import com.ko.spring.domin.CustomUser
import com.ko.spring.domin.MemberInfo
import com.ko.spring.domin.Response
import com.ko.spring.service.BotMessageService
import com.ko.spring.utils.getUser
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import java.io.File
import java.util.*


@Controller
@RequestMapping("/bot")
class BotController @Autowired constructor(private val botMessageService: BotMessageService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var myBot: MyBot
    @Autowired
    private lateinit var bot : Bot
    @Autowired
    private lateinit var pluginManager: PluginManager

    @GetMapping(value = ["/index", ""])
    fun index(): ModelAndView {
        val mv = ModelAndView("botIndex")
        val user = getUser()
        mv.addObject("user", user)
        mv.addObject("ready", myBot.ready)

        mv.addObject("plugins", pluginManager.plugins.map {
            PluginManager.PluginInfo(it.value)
        })
        if (myBot.ready) {
            mv.addObject("id", bot.id)
            mv.addObject("nick", bot.nick)
            mv.addObject("time", myBot.getTime())

            if (user!=null) {
                mv.addObject("groups", bot.groups.filter { hasAuth(it.id) })
            } else {
                mv.addObject("groups", emptyList<Group>())
            }
        }
        return mv
    }

    @PostMapping("/lastMessage")
    @ResponseBody
    fun lastMessage(@RequestParam("groupID") groupID: Long): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        val msg = botMessageService.lastMessage(groupID) ?: return Response(400, "暂时无法获得！")
        return Response(msg)
    }

    @PostMapping("/sentMessages")
    @ResponseBody
    fun sentMessages(@RequestParam groupID: Long): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        return Response(botMessageService.getSentMessages(groupID))
    }

    @PostMapping("/recallSent")
    @ResponseBody
    suspend fun recallSent(@RequestParam groupID: Long, @RequestParam hash: Int): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        return if (botMessageService.recallMessage(hash, groupID)) {
            Response("成功")
        } else
            Response(400, "失败。")
    }

    @PostMapping("/repeat")
    @ResponseBody
    suspend fun repeat(@RequestParam("groupID") groupID: Long,
                        @RequestParam("hash") hash: Int): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        val res = botMessageService.repeat(groupID, hash)
        if (res < 0) {
            return Response(400, "错误！")
        }
        return Response(res)
    }

    @PostMapping("/nameCard")
    @ResponseBody
    fun nameCard(@RequestParam("groupID") groupID: Long,
                 @RequestParam("new", required = false) new: String?): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        val group = bot.getGroup(groupID) ?: return Response(400, "未知群id！")
        if (new == null) {
            return Response(group.botAsMember.nameCard)
        }
        group.botAsMember.nameCard = new
        return Response("已更新")
    }

    @PostMapping("/recall")
    @ResponseBody
    suspend fun recall(@RequestParam("hash") hash: Int) {
        botMessageService.recall(hash)
    }

    @GetMapping("/getMembers")
    @ResponseBody
    fun getMembers(@RequestParam("groupID") groupID: Long): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        val group = bot.getGroup(groupID) ?: return Response(400, "未知群id！")
        val list = mutableListOf<MemberInfo>()
        group.members.forEach { list.add(MemberInfo(it)) }
        return Response(list)
    }

    @PostMapping("/sendText", consumes = ["text/plain"])
    @ResponseBody
    suspend fun sendText(
        @RequestParam("groupID") groupID: Long,
        @RequestBody text: String,
        authentication: Authentication
    ): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")

        val user = authentication.principal as CustomUser
        logger.info("send text {} to {} by {}({})", text, groupID, user.name, user.id)
        val res = botMessageService.sendText(groupID, text)

        if (res < 0) {
            return Response(400, "错误！")
        }
        return Response(res)
    }

    @ResponseBody
    @PostMapping("/sendImgByUrl")
    suspend fun sendImageByUrl(
        @RequestParam("group") groupID: Long,
        @RequestParam("url") url: String,
        @RequestParam("r18") r18: Boolean = false
    ): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        if (!myBot.ready)
            return Response(500, "机器人未就绪！")
        val res = botMessageService.sendByUrl(groupID, url, r18)
        return if (res > 0)
            Response(res)
        else
            Response(400, "错误！")
    }

    @PostMapping("/uploadFile", consumes = ["multipart/form-data"])
    @ResponseBody
    suspend fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("group") groupID: Long,
        @RequestParam("r18") r18: Boolean = false
    ): Response {
        if (!hasAuth(groupID)) return Response(403, "无权访问！")
        if (!myBot.ready)
            return Response(500, "机器人未就绪！")
        if (file.size == 0L) {
            return Response(400, "空文件!")
        }
        val name = file.originalFilename?.replace("blob", "jpg")

        var lf = File("image/${name ?: "image.jpg"}")
        val pre = lf.name.split(".").last()
        while (lf.exists()) {
            lf = File("image/${UUID.randomUUID()}.$pre")
        }

        val user = getUser()
        return try {
            file.transferTo(lf)
            val res = botMessageService.sendImgByFile(groupID, lf, r18)
            logger.info("send image {} to {} by {}({})", lf.name, groupID, user?.name, user?.id)
            if (res > 0)
                Response(res)
            else
                Response(400, "错误！")
        } catch (e: Exception) {
            Response(400, "失败！")
        }
    }

    @ResponseBody
    @GetMapping("/plugin")
    fun plugin(): Response {
        return Response(pluginManager.plugins.map {
            PluginManager.PluginInfo(it.value)
        })
    }

    private fun hasAuth(groupID: Long): Boolean {
        val user = getUser() ?: return false
        if (user.hasAuth("AUTH_SUPER_ADMIN")) return true
        if (user.hasGroupAuth(groupID)) {
            return true
        }
        return false
    }
}