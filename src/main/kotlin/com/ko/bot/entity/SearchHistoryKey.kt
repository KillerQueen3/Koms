package com.ko.bot.entity

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class SearchHistoryKey(
    var groupID: Long,
    @Column(columnDefinition = "char(32)")
    var tagMd5: String) : Serializable {
    constructor() : this(0, "")
}
