package com.ko.bot.utils

import com.ko.bot.exception.SendImageException
import net.dreamlu.mica.http.HttpRequest
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.OkHttpClient
import org.jetbrains.annotations.Nullable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import javax.imageio.ImageIO

/**
 * 工具类。
 */
object Utils {
    init {
        val client = OkHttpClient().newBuilder().dns(IPV4DNS()).connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(15)).build()
        HttpRequest.setHttpClient(client)
    }

    /**
     * 获取来自群的 [图片][image] 的url。
     */
    @JvmStatic
    fun getImageURL(image: Image): String {
        return "http://gchat.qpic.cn/gchatpic_new/0/0-0-${
            image.imageId.substring(1, 37).replace("-".toRegex(), "")
        }/0?term=2"
    }

    /**
     * 将 [BufferedImage] 转为 [ExternalResource]。
     */
    @JvmStatic
    @Throws(Exception::class)
    fun bufferedImageToResource(image: BufferedImage): ExternalResource {
        val stream = ByteArrayOutputStream()
        ImageIO.write(image, "png", stream)
        stream.flush()
        val res = stream.toByteArray()
        val resource: ExternalResource = res.toExternalResource()
        stream.close()
        return resource
    }


    /**
     * 将图片url转为 [ExternalResource]。
     * @throws SendImageException 返回code非200
     */
    @JvmStatic
    @Throws(Exception::class)
    fun getResourceFromUrl(url: String): ExternalResource {
        val bytes = HttpRequest.get(url)
            .retry(2, 500)
            .execute().onResponse {
                if (it.code() != 200) throw SendImageException("${it.code()} ${it.message()}")
                return@onResponse it.asBytes()
            }
        return ByteArrayInputStream(bytes).toExternalResource()
    }

    /**
     * 将图片url转为 [BufferedImage]。
     * @throws SendImageException 返回code非200
     */
    @Nullable
    @JvmStatic
    fun getBufferedImageFromUrl(url: String): BufferedImage? {
        val bytes = HttpRequest.get(url)
            .retry(2, 500)
            .execute().onResponse {
                if (it.code() != 200) throw SendImageException("${it.code()} ${it.message()}")
                return@onResponse it.asBytes()
            }

        return ImageIO.read(ByteArrayInputStream(bytes))
    }

    /**
     * 在 [source] 左上角画一个白色像素。
     */
    @JvmStatic
    fun r18Image(source: BufferedImage) {
        val g = source.graphics
        g.drawRect(0, 0, 1, 1)
        g.dispose()
    }
}