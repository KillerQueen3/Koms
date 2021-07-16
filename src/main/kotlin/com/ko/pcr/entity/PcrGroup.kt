package com.ko.pcr.entity

import net.mamoe.mirai.contact.Group
import javax.persistence.*

@Entity
data class PcrGroup(
    @Id
    val groupId: Long,

    val name: String,
) {

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId")
    val members: MutableSet<PcrMember> = mutableSetOf()

    constructor() : this(0, "")

    constructor(group: Group) : this(group.id,  group.name)

    constructor(groupId: Long): this(groupId,  "")
}
