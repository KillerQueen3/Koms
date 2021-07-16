package com.ko.spring.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File
import java.io.FileInputStream

@Controller
class MiscController {
    @GetMapping(
        "/background/randomImage",
        produces = [MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE]
    )
    @ResponseBody
    fun randomImage(
        @RequestParam("group") group: Int,
        @RequestParam("h", required = false) h: Boolean = false
    ): ByteArray {
        var i = group
        if (i > 4 || i < 1) {
            i = 1
        }

        return if (!h) {
            val file = File("./image/bg/$i").listFiles()!!.random()
            FileInputStream(file).readAllBytes()
        } else {
            val file = File("./image/h/$i").listFiles()!!.random()
            FileInputStream(file).readAllBytes()
        }
    }
}