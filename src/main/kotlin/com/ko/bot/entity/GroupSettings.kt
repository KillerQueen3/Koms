package com.ko.bot.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class GroupSettings(
    @Id
    val groupId: Long
) {
    @Column(length = 500)
    var disabledPlugins = ""
    var pureMode = false
    var moreCmd = ""

    constructor() : this(0)

    override fun toString(): String {
        return "GroupSettings(groupId=$groupId, disabledPlugins='$disabledPlugins', pureMode=$pureMode, moreCmd='$moreCmd')"
    }
}
