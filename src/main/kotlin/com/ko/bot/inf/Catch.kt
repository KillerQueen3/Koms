package com.ko.bot.inf

/**
 * 消息处理函数使用的注解。
 * @param entry 消息触发此方法需要满足的正则表达式
 * @param listen 监听来源类型
 * @param permission 需要的权限
 * @param nologging 为true时触发此方法不记录日志
 * @param removeRegexes 消息将删除掉其中匹配的正则，注入方法的String参数中。
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class Catch(val entry: String, val listen: Int = 0, val permission: Int = 2, val nologging: Boolean = false, val removeRegexes: Array<String> = []) {
    companion object {
        const val ON_GROUP = 0
        const val ON_FRIEND = 1
        const val OWNER = 0
        const val ADMIN = 1
        const val MEMBER = 2
        const val SUPER_USER = 3
    }
}
