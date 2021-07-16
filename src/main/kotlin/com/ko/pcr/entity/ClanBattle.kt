package com.ko.pcr.entity

import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.util.*
import javax.persistence.*

@Entity
data class ClanBattle(
    @Id
    @GeneratedValue
    val id: Int,
    val name: String,

    @ManyToOne
    @JoinColumn(name = "groupId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    val group: PcrGroup
) {
    constructor() : this(0, "", PcrGroup())

    constructor(name: String, group: PcrGroup): this(0, name, group)

    var comment: String = ""
    var createDate: Date = Date()
}
