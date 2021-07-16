package com.ko.spring.domin

import com.ko.spring.utils.toAuthList
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class CustomUser(
    @Id
    val id: String,
    val name: String,
    var pw: String,
    val auth: String,
    val groupAuth: String
) : UserDetails {
    private var accountNonExpired = true
    private var accountNonLocked = true
    private var credentialsNonExpired = true
    private var enabled = true
    var logined = false

    constructor() : this("0", "", "", "", "")

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return auth.toAuthList()
    }

    override fun getPassword(): String {
        return pw
    }

    override fun getUsername(): String {
        return id
    }

    override fun isAccountNonExpired(): Boolean {
        return accountNonExpired
    }

    override fun isAccountNonLocked(): Boolean {
        return accountNonLocked
    }

    override fun isCredentialsNonExpired(): Boolean {
        return credentialsNonExpired
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    fun hasAuth(needAuth: String): Boolean = auth.split(",").contains(needAuth)

    fun hasGroupAuth(groupId: Long) = groupAuth.split(",").contains(groupId.toString())
}