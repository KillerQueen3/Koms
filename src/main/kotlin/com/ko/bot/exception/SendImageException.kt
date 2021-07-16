package com.ko.bot.exception

class SendImageException(msg: String) : RuntimeException(msg) {
    constructor() : this("")
}
