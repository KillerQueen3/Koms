package com.ko.bot.entity

import com.ko.spring.utils.toMd5
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class SearchHistory constructor(
    @EmbeddedId
    @Column(name = "history_key")
    val key: SearchHistoryKey,
    @Column(length = 8000)
    var history: String
) {
    constructor() : this(SearchHistoryKey(0, "d41d8cd98f00b204e9800998ecf8427e"), "")

    constructor(groupID: Long, tag: String, history: String) : this(SearchHistoryKey(groupID, tag.toMd5()), history)

    constructor(groupID: Long, tag: String) : this(SearchHistoryKey(groupID, tag.toMd5()), "")

    fun toList(): MutableList<Long> {
        val res = mutableListOf<Long>()
        if (history.isEmpty())
            return res
        val split = history.split(",")
        for (l in split) {
            try {
                res.add(l.toLong())
            } catch (e: NumberFormatException) {

            }
        }
        return res
    }

    fun clear() {
        history = ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchHistory

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}