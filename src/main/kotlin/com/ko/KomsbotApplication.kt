package com.ko

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.File

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
open class KomsbotApplication

fun main(args: Array<String>) {
    runApplication<KomsbotApplication>(*args)

    val f = File("./image")
    if (!f.exists())
        f.mkdirs()
}

