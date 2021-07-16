package com.ko.bot.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * 标签映射数据类。
 */
@Entity
data class TagMapping (
    /**
     * 唯一标识符，自动生成。
     */
    @Id
    @GeneratedValue
    val id: Int,
    /**
     * 使用此映射时，输入需满足的正则表达式。
     */
    val regex: String,
    /**
     * 映射输出标签，使用 "," 隔开多个。
     */
    @Column(length = 500)
    val tags: String
) {
    constructor(regex: String, tags: String) : this(0, regex, tags)
    constructor() : this(0, "", "")
    constructor(id: Int) : this(id, "", "")
}
