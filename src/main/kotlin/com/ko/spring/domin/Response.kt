package com.ko.spring.domin

data class Response(val code: Int, val msg: Any) {
    constructor(msg: Any) : this(200, msg)
}
