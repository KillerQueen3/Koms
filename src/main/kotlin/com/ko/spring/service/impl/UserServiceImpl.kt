package com.ko.spring.service.impl

import com.ko.spring.dao.UserDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserServiceImpl @Autowired constructor(private val userDao: UserDao) : UserDetailsService {
    val logger: Logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override fun loadUserByUsername(name: String?): UserDetails {
        return userDao.getUser(name!!) ?: throw UsernameNotFoundException("该用户不存在")
    }
}