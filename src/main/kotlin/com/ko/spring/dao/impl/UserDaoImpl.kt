package com.ko.spring.dao.impl

import com.ko.spring.dao.UserDao
import com.ko.spring.domin.CustomUser
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UserDaoImpl @Autowired constructor(private val factory: SessionFactory) :UserDao  {
    override fun getUser(name: String):CustomUser? {
        val session = factory.currentSession
        session.beginTransaction()
        val c = session.get(CustomUser::class.java, name)
        session.transaction.commit()
        return c
    }

    override fun addUser(user: CustomUser) {
        val session = factory.currentSession
        val b = session.beginTransaction()
        session.save(user)
        b.commit()
    }
}