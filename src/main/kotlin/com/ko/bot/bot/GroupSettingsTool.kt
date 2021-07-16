package com.ko.bot.bot

import com.ko.bot.entity.GroupSettings
import net.mamoe.mirai.Bot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class GroupSettingsTool {
    @Autowired
    private lateinit var bot: Bot

    @Autowired
    private lateinit var botDBHelper: BotDBHelper

    @PostConstruct
    fun addSettings() {
         bot.groups.forEach {
            changeSettings(getSettings(it.id))
         }
    }

    fun getSettings(groupID:Long): GroupSettings {
        return botDBHelper.getSomething(GroupSettings::class.java, groupID)?:return GroupSettings(groupID)
    }

    fun changeSettings(settings: GroupSettings) {
        botDBHelper.saveOrUpdateSomething(settings)
    }

    fun getAllSettings() = botDBHelper.sqlQuery("FROM GroupSettings", GroupSettings::class.java)

    fun setMore(id: Long, cmd: String) {
        botDBHelper.sqlUpdate("UPDATE GroupSettings SET moreCmd = '$cmd' WHERE groupId = $id ")
    }
}