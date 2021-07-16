package com.ko.bot.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Trans(
    @Id
    @Column(columnDefinition = "varchar(100)")
    val ch: String,
    val jp: String
) {
    constructor() : this("", "")
}