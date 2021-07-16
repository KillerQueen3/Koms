package com.ko.pcr.clan

import com.ko.bot.bot.BotDBHelper
import com.ko.pcr.entity.ClanBattle
import com.ko.pcr.entity.PcrGroup
import com.ko.pcr.entity.PcrMember
import com.ko.pcr.entity.Reservation
import com.ko.pcr.exceptions.PcrException
import io.ktor.util.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ClanCenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var botDBHelper: BotDBHelper
    @Autowired
    private lateinit var pcrTools: PCRTools

    fun cmd(member: Member, group: Group, type: ClanCmd, param: String): String {
        try {
            if (type == ClanCmd.CREATE_CLAN) {
                return createClan(PcrGroup(group))
            }
            if (type == ClanCmd.JOIN_CLAN) {
                return joinClan(PcrMember(member), pcrTools.getPcrGroup(group))
            }
            val pcrMember = pcrTools.getPcrMember(member)
            val pcrGroup = pcrTools.getPcrGroup(group)

            return when(type) {
                ClanCmd.RESERVE -> reserve(pcrMember, pcrGroup, param)
                ClanCmd.CREATE_CLAN_BATTLE -> createClanBattle(pcrGroup, param)
                else -> "未知指令。"
            }
        } catch (e: PcrException) {
            return e.message?: "错误！"
        } catch (e: Exception) {
            logger.error(e)
            return "错误！"
        }

    }

    private fun createClanBattle(group: PcrGroup, param: String): String {
        val battle = ClanBattle(param, group)
        botDBHelper.saveOrUpdateSomething(battle)
        return "成功！"
    }

    private fun createClan(group: PcrGroup): String {
        val exist = botDBHelper.getSomething(PcrGroup::class.java, group.groupId)
        return if (exist == null) {
            botDBHelper.saveOrUpdateSomething(group)
            "完成！"
        } else {
            "公会已存在"
        }
    }

    private fun joinClan(member: PcrMember, group: PcrGroup): String {
        val exist = botDBHelper.getSomething(PcrMember::class.java, member.id)
        if (exist != null) {
            return "失败：${exist.name}已经加入了一个公会（${exist.group.name}）。"
        }

        member.group = group
        botDBHelper.saveOrUpdateSomething(member)
        return "成功！"
    }

    private fun reserve(member: PcrMember, group: PcrGroup, param: String): String {
        val params = param.split("[:：]".toRegex())
        if (params.isEmpty()) return "请输入正确的参数！"
        val boss = params[0].toInt()
        if (boss <= 0 || boss >5) {
            return "错误的编号：$boss"
        }

        val exist = pcrTools.queryReservation(group.groupId, member.id, boss)
        if (exist.isNotEmpty())
            return "你已经预约过了。"

        val reserve = Reservation(member, group, boss)
        if (params.size > 1)
            reserve.message = params[1]
        botDBHelper.saveOrUpdateSomething(reserve)
        return "预约成功！"
    }

    private fun applyChallenge(member: PcrMember, group: PcrGroup, param: String): String {
        return ""
    }

}