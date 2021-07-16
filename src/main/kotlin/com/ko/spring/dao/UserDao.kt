package com.ko.spring.dao

import com.ko.spring.domin.CustomUser

interface UserDao {
    fun getUser(name: String):CustomUser?
    fun addUser(user: CustomUser)
}