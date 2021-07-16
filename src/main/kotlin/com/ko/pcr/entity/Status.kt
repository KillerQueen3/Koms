package com.ko.pcr.entity

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Status (
    @Id
    val groupId: Long,
) {
    constructor() : this(0)

    var cycle: Int = 0
    var boss1: Int = 0
    var boss2: Int = 0
    var boss3: Int = 0
    var boss4: Int = 0
    var boss5: Int = 0
}
