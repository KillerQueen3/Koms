package com.ko.spring.domin

import net.mamoe.mirai.contact.Member

data class MemberInfo(
    val id: Long,
    val nick: String,
    val nameCard: String,
    val permission: String,
    val avatarUrl: String) {
    constructor(member: Member) : this(member.id, member.nick, member.nameCard, member.permission.name, member.avatarUrl)
}