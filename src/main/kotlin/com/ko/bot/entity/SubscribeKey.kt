package com.ko.bot.entity

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class SubscribeKey(
    var groupID: Long,
    var artistUid: Long
) : Serializable {
    constructor() : this(0, 0)
}