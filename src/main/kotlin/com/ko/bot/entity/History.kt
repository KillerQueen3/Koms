package com.ko.bot.entity

import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * 图片搜索记录，用于数据分析。
 */
@Entity
data class History(
    val nick: String,
    val qqID: Long,
    val groupName: String,
    val groupID: Long,
    val cmd: String?,
    val type: String,
    val resultPid: Long
    ) {
    @Id
    @GeneratedValue
    val id: Int = 0
    val date: Date = Date()

    constructor() : this("", 0, "", 0, null, "", 0)

    constructor(nick: String, qqID: Long, groupName: String, groupID: Long, type: String, resultPid: Long):
            this(nick, qqID, groupName, groupID, null, type, resultPid)
}