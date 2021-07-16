package com.ko.spring.service

import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dreamlu.mica.http.DomMapper
import net.dreamlu.mica.http.HttpRequest
import net.mamoe.mirai.Bot
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTasks {
    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Autowired private lateinit var bot: Bot

    @Scheduled(cron = "0 0 0,12 * * *")
    fun tbGoogle10000YenPriceAutoSend() {
        val arrayGP = arrayOf(
            "https://item.taobao.com/item.htm?spm=a230r.1.14.15.38291062SAMEgL&id=610812985335&ns=1&abbucket=17#detail",
            "https://item.taobao.com/item.htm?spm=a230r.1.14.15.488d687a8s446f&id=532610203990&ns=1&abbucket=17#detail"
        )
        try {
            val listG = mutableListOf<String>()
            for (url in arrayGP) {
                val response = HttpRequest.get(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36")
                    .execute().asString()
                val document = DomMapper.readDocument(response)
                val price = document.getElementById("J_StrPriceModBox").getElementsByClass("tb-rmb-num")
                if (price.isEmpty()) {
                    logger.warn("something wrong!")
                    return
                }
                listG.add(price[0].text())
                Thread.sleep(100)
            }

            val target = bot.getFriend(845611232) ?: return
            GlobalScope.launch {
                target.sendMessage("GP10000：当前价格为${listG.toString().replace("[\\[\\]]".toRegex(), "")}")
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }
}