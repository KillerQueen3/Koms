package com.ko.spring.service

import com.ko.bot.entity.SubscribeArtist
import com.ko.bot.img.HyperImageSearch
import com.ko.bot.img.SetuService
import com.ko.bot.bot.BotDBHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PixivArtistSubscribeService {
    @Autowired private lateinit var hyperImageSearch: HyperImageSearch
    @Autowired private lateinit var bot: Bot
    @Autowired private lateinit var service: SetuService
    @Autowired private lateinit var botDBHelper: BotDBHelper

    fun addSubscribe(groupID: Long, artistUid: Long): Boolean {
        val check = hyperImageSearch.checkArtist(artistUid) ?: return false
        if (check.num == 0) return false
        hyperImageSearch.subscribe(groupID, artistUid)
        return true
    }

    fun deleteSubscribe(groupID: Long, artistUid: Long, delete: Boolean): Boolean {
        if (delete) {
            botDBHelper.deleteSomething(SubscribeArtist(groupID, artistUid))
        } else {
            hyperImageSearch.subscribe(groupID, artistUid, true)
        }
        return true
    }

    fun getSubscribes(): Map<Long, List<Long>> {
        val subs = botDBHelper.getSubscribes()
        val res = mutableMapOf<Long, MutableList<Long>>()
        for (s in subs) {
            if (res.containsKey(s.subscribeKey.groupID)) {
                res[s.subscribeKey.groupID]?.add(s.subscribeKey.artistUid)
            } else {
                res[s.subscribeKey.groupID] = mutableListOf(s.subscribeKey.artistUid)
            }
        }
        return res
    }

    @Scheduled(cron = "0 0 * * * *")
    fun sendSubscribed() {
        val prepared = hyperImageSearch.prepareSubscribe()
        for ((groupID, news) in prepared) {
            val group = bot.getGroup(groupID) ?: continue
            for (p in news) {
                GlobalScope.launch {
                    service.sendImage(p, group, false) }
                Thread.sleep(1000)
            }
        }
    }
}