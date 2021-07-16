package com.ko.bot.entity

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class SubscribeArtist(
    @EmbeddedId
    val subscribeKey: SubscribeKey,
    val isActive: Boolean) {
    constructor() : this(SubscribeKey(), false)

    constructor(groupID: Long, artistUid: Long) : this(SubscribeKey(groupID, artistUid), false)
}
