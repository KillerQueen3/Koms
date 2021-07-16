package com.ko.spring.config

import org.hibernate.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataBaseConfig {
    @Bean
    fun sessionFactory(): SessionFactory {
        return org.hibernate.cfg.Configuration().configure().buildSessionFactory()
    }
}
