package com.ko.pcr.clan

import com.ko.bot.bot.BotDBHelper
import com.ko.pcr.entity.*
import com.ko.pcr.exceptions.PcrException
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class PCRTools {
    @Autowired private lateinit var botDBHelper: BotDBHelper

    companion object {
        fun getPcrDate(date: Date = Date()): Int {
            val c = Calendar.getInstance()
            c.time = date
            var day = c.get(Calendar.DAY_OF_YEAR)
            if (c.get(Calendar.HOUR_OF_DAY) < 4) day -= 1

            return c.get(Calendar.YEAR) * 1000 + day
        }
    }

    fun queryToday(group: Long) = botDBHelper.sqlQuery("FROM Challenge WHERE pcrDate = ${getPcrDate()} AND groupID = $group", Challenge::class.java)

    fun queryToday(group: Long, member: Long) = botDBHelper.sqlQuery("FROM Challenge WHERE pcrDate = ${getPcrDate()} AND memberId = $member AND groupID = $group", Challenge::class.java)

    fun queryReservation(group: Long, member: Long = -1, boss: Int = -1): MutableList<Reservation> {
        var hql = "FROM Reservation WHERE groupId = $group "
        if (member > 0)
            hql += "AND memberId = $member "
        if (boss > 0)
            hql += "AND boss = $boss"

        return botDBHelper.sqlQuery(hql, Reservation::class.java)
    }

    fun getPcrGroup(group: Group) = botDBHelper.getSomething(PcrGroup::class.java, group.id) ?: throw PcrException("${group.name} 未创建工会！")

    fun getPcrMember(member: Member) = botDBHelper.getSomething(PcrMember::class.java, member.id) ?: throw PcrException("${member.nameCardOrNick} 未加入工会！")

    fun getClanBattle(pcrGroup: PcrGroup) =
        botDBHelper.sqlQuery("FROM ClanBattle WHERE groupId = ${pcrGroup. groupId} ORDER BY createDate DESC", ClanBattle::class.java, 1)
            .firstOrNull()?: throw PcrException("未创建工会战！")

}
