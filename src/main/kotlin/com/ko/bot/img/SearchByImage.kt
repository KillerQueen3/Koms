package com.ko.bot.img

import io.ktor.util.*
import net.dreamlu.mica.http.DomMapper
import net.dreamlu.mica.http.HttpRequest
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 以图搜图工具类，使用ascii2d.net搜索。
 */
object SearchByImage {
    private val logger = LoggerFactory.getLogger(SearchByImage::class.java)

    private fun getInfo(requestURL: String): String? {
        logger.info("Getting {}", requestURL)
        try {
            val response = HttpRequest.get(requestURL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36")
                .retry(10, 400).execute().asString()
            if (response == null || response.isEmpty()) {
                logger.warn("FAILED! RESPONSE: {}", response)
                return null
            }
            return response
        } catch (e: Exception) {
            logger.error(e)
        }
        return null
    }

    /**
     * ascii2d色合检索。
     */
    private fun searchA(imgURL: String): Array<String>? {
        val response = getInfo("https://ascii2d.net/search/url/" + URLEncoder.encode(imgURL, StandardCharsets.UTF_8)) ?: return null
        val document = DomMapper.readDocument(response)
        val itemBoxes = document.getElementsByClass("row item-box")
        var item1 = itemBoxes.first()
        val hash = item1.getElementsByClass("hash").first().text()
        val bovw = "https://ascii2d.net/search/bovw/$hash"
        if (!item1.getElementsByClass("detail-box gray-link").first().hasText()) {
            item1 = itemBoxes[1]
        }
        val imgBox = item1.getElementsByClass("col-xs-12 col-sm-12 col-md-4 col-xl-4 text-xs-center image-box").first()
        return try {
            val src = imgBox.getElementsByTag("img").first().attr("src")
            val source = item1.getElementsByTag("a").first().attr("href")
            arrayOf("https://ascii2d.net$src", source, bovw)
        } catch (e: NullPointerException) {
            logger.error(e)
            null
        }
    }

    /**
     * ascii2d特征检索。
     */
    private fun searchABovw(bovw: String): Array<String>? {
        val response = getInfo(bovw) ?: return null
        val document = DomMapper.readDocument(response)
        val itemBoxes = document.getElementsByClass("row item-box")
        var item1 = itemBoxes.first()
        if (!item1.getElementsByClass("detail-box gray-link").first().hasText()) {
            item1 = itemBoxes[1]
        }
        val imgBox = item1.getElementsByClass("col-xs-12 col-sm-12 col-md-4 col-xl-4 text-xs-center image-box").first()
        return try {
            val src = imgBox.getElementsByTag("img").first().attr("src")
            val source = item1.getElementsByTag("a").first().attr("href")
            arrayOf("https://ascii2d.net$src", source)
        } catch (e: NullPointerException) {
            logger.error(e)
            null
        }
    }

    fun searchAscii2d(url: String): Array<Array<String>?>? {
        logger.info("Search by image: {}", url)
        val c = searchA(url) ?: return null
        val b = searchABovw(c[2])
        return arrayOf(c, b)
    }
}