package com.ko.spring.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig @Autowired constructor(
    @Qualifier("userServiceImpl") val userService: UserDetailsService
) : WebSecurityConfigurerAdapter() {
    @Bean
    fun passwordEncoder(): PasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(http: HttpSecurity?) {
        http?.formLogin()?.loginPage("/login")?.failureUrl("/login?error")?.defaultSuccessUrl("/")
            ?.and()?.rememberMe()?.rememberMeServices(getRememberMeServices())?.key(SECRET_KEY)
            ?.and()?.authorizeRequests()
            ?.antMatchers("/bot/*", "/bot")?.hasAnyAuthority("AUTH_ADMIN", "AUTH_SUPER_ADMIN")
            ?.antMatchers("/setu/*", "/setu")?.hasAnyAuthority("AUTH_SUPER_ADMIN")
            ?.antMatchers("/admin/*", "/admin")?.hasAnyAuthority( "AUTH_SUPER_ADMIN")
            ?.antMatchers("/article/edit/*", "/article/new", "/article/edit")?.hasAnyAuthority("AUTH_ADMIN", "AUTH_SUPER_ADMIN")
            ?.and()?.csrf()?.ignoringAntMatchers("/login", "/logout")
    }

    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth?.userDetailsService(userService)?.passwordEncoder(passwordEncoder())
    }

    override fun configure(web: WebSecurity?) {
        super.configure(web)
    }

    companion object {
        private const val SECRET_KEY = "1145141919810"
    }

    private fun getRememberMeServices(): TokenBasedRememberMeServices {
        val services = TokenBasedRememberMeServices(SECRET_KEY, userService)
        services.setCookieName("remember-cookie")
        services.setTokenValiditySeconds(31536000)
        return services
    }
}