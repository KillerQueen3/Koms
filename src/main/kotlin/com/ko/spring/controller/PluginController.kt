package com.ko.spring.controller

import com.ko.spring.utils.getUser
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import java.io.File
import java.io.FileReader

@Controller
@RequestMapping("/plugin")
class PluginController {
    @RequestMapping("/document/{name}")
    fun pluginDoc(@PathVariable name: String): ModelAndView {
        val docFile = File("./plugins/${name}.html")
        if (!docFile.exists()) return ModelAndView("error/articleNotFound")
        val mv = ModelAndView("pluginDoc")
        mv.addObject("user", getUser())
        mv.addObject("title", "插件 $name 说明文档")
        val reader = FileReader(docFile)
        mv.addObject("html", reader.readText())
        reader.close()
        return mv
    }
}