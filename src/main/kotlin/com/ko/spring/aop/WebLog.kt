package com.ko.spring.aop

import com.ko.bot.entity.PixivImage
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.*

@Aspect
@Component
class WebLog {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class PixivLog(val addr: String, val method: String, val arg: String, val startTime: Long)
    val threadLocal: ThreadLocal<PixivLog> = ThreadLocal()

    @Pointcut("execution(com.ko.bot.entity.PixivImage com.ko.spring.controller.PixivController.*(..))")
    fun pLog() {}

    @Before("pLog()")
    fun pBefore(joinPoint: JoinPoint) {
        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
        val request = attributes!!.request

        val name = joinPoint.signature.name
        threadLocal.set(PixivLog(request.remoteAddr, name, joinPoint.args.firstOrNull().toString(), System.currentTimeMillis()))
    }

    @AfterReturning(returning = "ret", pointcut = "pLog()")
    fun pReturn(ret: Any) {
        if (ret !is PixivImage)
            return
        val pl = threadLocal.get()
        logger.info("{} [{}] from {}: start: {}, timeUsed: {} (s), result: {}", pl.method, pl.arg, pl.addr, Date(pl.startTime),
            (System.currentTimeMillis() - pl.startTime) / 1000, ret.pid)

        threadLocal.remove()
    }
}