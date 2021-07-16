package com.ko.pcr.entity

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import javax.persistence.*

@Entity
data class PcrMember (
    @Id
    val id: Long,

    var name: String,

    /**
     * 该成员的权限
     *
     * 1 - 普通成员
     *
     * 2 - 管理员
     *
     * 3 - 首领
     *
     * <=0 - 已废弃或禁用
     */
    var auth: Int
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId")
    lateinit var group: PcrGroup

    constructor() : this(0,  "", 0)

    constructor(member: Member): this(member.id, member.nameCardOrNick, 0) {
        auth = when(member.permission) {
            MemberPermission.ADMINISTRATOR -> 2
            MemberPermission.OWNER -> 3
            else -> 1
        }
    }

    constructor(id: Long): this(id, "", 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PcrMember

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
