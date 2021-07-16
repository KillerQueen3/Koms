package com.ko.spring.controller

import com.ko.spring.utils.getUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView
import java.io.File
import javax.annotation.PostConstruct

@Controller
class Page {
    @GetMapping("/login")
    fun login(): String {
        return "login"
    }

    @GetMapping("/test")
    fun test(): ModelAndView {
        val mv = ModelAndView("test")
        mv.addObject("user", getUser())
        return mv
    }


    private val animals = arrayListOf<String>()

    @PostConstruct
    fun initAnimals() {
        File("./audio/animal").listFiles()?.forEach { animals.add("/audio/animal/${it.name}") }
    }

    @GetMapping("/fish")
    fun fish(): ModelAndView {
        val mv = ModelAndView("fish")
        mv.addObject("animals", animals)
        mv.addObject("user", getUser())
        return mv
    }

    @GetMapping("/index", "")
    fun index(): ModelAndView {
        val mv = ModelAndView("index")
        val user = getUser() ?: return mv
        mv.addObject("user", user)
        return mv
    }

    @GetMapping("/coffee")
    @ResponseBody
    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    fun coffee(): String {
        return "I'm a teapot!"
    }
}