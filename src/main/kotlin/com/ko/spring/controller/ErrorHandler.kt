package com.ko.spring.controller

import com.ko.bot.entity.PixivImage
import com.ko.bot.exception.SendImageException
import com.ko.spring.domin.Response
import com.ko.spring.exception.ArticleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.DisabledException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.IOException
import java.io.InterruptedIOException
import java.lang.IllegalArgumentException
import java.net.MalformedURLException

@ControllerAdvice
class ErrorHandler {
    private val logger: Logger = LoggerFactory.getLogger(ErrorHandler::class.java)

    @ExceptionHandler(Exception::class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    fun error(e :Exception) {
        logger.error(e.message, e)
    }

    @ExceptionHandler(IOException::class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    fun ignore() {

    }

    @ExceptionHandler(SendImageException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ResponseBody
    fun sendImageError(): PixivImage {
        return PixivImage.API_ERROR
    }

    @ResponseBody
    @ExceptionHandler(MalformedURLException::class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    fun url(): Response {
        return Response(400, "无效的URL")
    }

    @ResponseBody
    @ExceptionHandler(InterruptedIOException::class)
    @ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
    fun timeOut(): Response {
        return Response(504, "超时")
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    fun bad(): Response {
        return Response(400, "BAD REQUEST")
    }

    @ResponseBody
    @ExceptionHandler(ArticleException::class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    fun articleError(e: Exception) : Response {
        return Response(400, e.message?:"错误！")
    }
}