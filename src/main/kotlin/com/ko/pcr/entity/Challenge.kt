package com.ko.pcr.entity

import com.ko.pcr.clan.PCRTools
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.util.*
import javax.persistence.*

@Entity
data class Challenge(
    @Id
    @GeneratedValue
    val id: Int,
    val stage: Int,
    val boss: Int,
    val time: Date,
    val cycle: Int,
    val damage: Int,
    val remain: Int,
    val isContinue: Boolean,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "memberId")
    val member: PcrMember,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "groupId")
    val group: PcrGroup,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "battleId")
    val battle: ClanBattle
) {
    constructor() : this(0, 0, 0, Date(), 0, 0,  0, false, PcrMember(), PcrGroup(), ClanBattle())

    constructor(stage: Int, boss: Int, cycle: Int, damage: Int, remain: Int, isContinue: Boolean, member: PcrMember, group: PcrGroup, battleID: ClanBattle):this(
        0, stage, boss, Date(), cycle, damage, remain, isContinue, member, group, battleID
    )

    var pcrDate: Int = PCRTools.getPcrDate()
    var number: Int = 0
}
