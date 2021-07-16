package com.ko.pcr.entity

import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.util.*
import javax.persistence.*

@Entity
data class Reservation(
    @Id
    @GeneratedValue
    val id: Int,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "memberId")
    val member: PcrMember,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "groupId")
    val group: PcrGroup,

    val boss: Int,
) {
    constructor() : this(0, PcrMember(), PcrGroup(), 0)

    constructor(member: PcrMember, group: PcrGroup, boss: Int) : this(0, member, group, boss)

    var message: String = ""
    val date = Date()
}
