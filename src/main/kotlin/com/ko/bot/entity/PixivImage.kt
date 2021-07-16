package com.ko.bot.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Transient

/**
 * 来自pixiv的图片信息。
 */
@Entity
data class PixivImage(
    @Id
    /**
     * 图片pid，唯一标识符。小于0代表错误信息。
     */
    var pid: Long = 0
) {
    /**
     * 该pid下的图片数量。
     */
    var p: Int = 0

    /**
     * 作者uid。
     */
    var uid: Long = 0

    /**
     * 标题。
     */
    var title: String? = null

    /**
     * 作者名字。
     */
    var author: String? = null

    /**
     * 小图url。
     */
    var url: String? = null

    /**
     * 原图url。
     */
    var originalUrl: String? = null

    /**
     * 是否r18。
     * 通过 tag 中是否含 R-18 判断。
     * **可能不准确！**
     */
    var r18: Boolean = false

    /**
     * tag组成的字符串。
     * 由","隔开，每两个为一组，前一个是原文，后一个是翻译（若无则留空）。
     */
    @Column(length = 400)
    var tagStr: String? = null

    var type = ""

    constructor() : this(0)

    constructor(
        pid: Long,
        p: Int,
        uid: Long,
        title: String,
        author: String,
        url: String,
        originalUrl: String,
        r18: Boolean,
        tagStr: String?
    ) : this(pid) {
        this.p = p
        this.uid = uid
        this.title = title
        this.author = author
        this.url = url
        this.originalUrl = originalUrl
        this.r18 = r18
        this.tagStr = tagStr
    }

    /**
     * 得到此图片的信息。
     */
    fun infoString(): String {
        return """
     pid: $pid
     标题: $title${if (r18) " (R18)" else ""}
     作者: ${author!!.replace("@.*".toRegex(), "")} ($uid)
     """.trimIndent() +
                (if (p > 1) """
     
     有${p}张图片
     """.trimIndent() else "") +
                "\n原图: " + originalUrl
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PixivImage

        if (pid != other.pid) return false

        return true
    }

    override fun hashCode(): Int {
        return pid.hashCode()
    }

    override fun toString(): String {
        return "PixivImage(pid=$pid, p=$p, type=$type, uid=$uid, title=$title, author=$author, url=$url, originalUrl=$originalUrl, r18=$r18, tags='$tagStr')"
    }

    companion object {
        val NO_MORE_PICTURES = PixivImage(-1)
        val API_ERROR = PixivImage(-2)
        val TIME_OUT = PixivImage(-3)
        val NO_SEARCH_RESULT = PixivImage(-4)
        val NO_ARTIST_ILLUST = PixivImage(-5)
        val NO_SUCH_ILLUST = PixivImage(-6)
        val NO_SINGLE_IMAGE = PixivImage(-7)
        val NO_PURE_IMAGE = PixivImage(-8)
        val INTENTIONAL_ERROR = PixivImage(-200)
        val UNKNOWN_ERROR = PixivImage(-100)
    }
}