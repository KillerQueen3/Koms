package com.ko.spring.domin

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Article (
    @Id
    @Column(columnDefinition = "char(11)")
    val id: String,
    val lastEditTime: Date,
    val author: String,
    val authorID: String,
    @Column(columnDefinition = "TEXT")
    val html: String,
    val title: String,
    val tagS: String,
    val createTime: Date,
    var deleted: Boolean
) {

    constructor(): this("0", Date(), "","","", "", "", Date(), false)
    constructor(id: String, lastEditTime: Date, author: String, authorID: String, html: String, title: String, tagS: String)
    : this(id, lastEditTime, author, authorID, html, title, tagS, Date(), false)

}
