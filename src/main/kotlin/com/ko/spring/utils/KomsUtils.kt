package com.ko.spring.utils

import com.ko.bot.utils.Utils
import com.ko.spring.domin.CustomUser
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.source
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.DigestUtils
import java.nio.charset.StandardCharsets

fun String.toAuthList(): Collection<GrantedAuthority> {
   return AuthorityUtils.commaSeparatedStringToAuthorityList(this)
}

fun String.toMd5(): String {
   return DigestUtils.md5DigestAsHex(this.toByteArray(StandardCharsets.UTF_8))
}

fun getUser(): CustomUser? {
   val principal = SecurityContextHolder.getContext().authentication?.principal
   if (principal == null || principal !is CustomUser) {
      return null
   }
   principal.pw = "?"
   return principal
}

fun String.toTagList(): MutableList<String> {
   val list = this.split("[,，;；]".toRegex())
   val res = mutableListOf<String>()
   for (i in list) {
      if (i.trim().isNotEmpty()) {
         res.add(i.trim())
      }
   }
   return res
}

fun MessageChain.toHtml(group: Group) : String {
   val res = StringBuilder()
   val id = this.source.fromId
   val member = group[this.source.fromId]
   if (member != null) {
      val color = when (member.permission) {
         MemberPermission.ADMINISTRATOR -> "blue"
         MemberPermission.OWNER -> "red"
         MemberPermission.MEMBER -> "black"
      }
      res.append("<span style='color:${color}' title='$id'>${member.nameCardOrNick}</span>: \n")
   }
   for (i in this) {
      if (i is At) {
         res.append("<span title='${i.target}'>${i.getDisplay(group)}</span>")
      } else if (i is Image) {
         res.append("<span class='hidden-img'>[<a href='${Utils.getImageURL(i)}' target='_blank'>图片</a>]<img src='${Utils.getImageURL(i)}'></span>")
      } else {
         res.append(i.contentToString())
      }
   }
   return res.toString()
}

fun MessageReceipt<Group>.toHtml(): String {
   return buildString {
      for (i in source.originalMessage) {
         if (i is At) {
            append("<span title='${i.target}'>${i.getDisplay(target)}</span>")
         } else if (i is Image) {
            append("<span class='hidden-img'>[<a href='${Utils.getImageURL(i)}' target='_blank'>图片</a>]<img src='${Utils.getImageURL(i)}'></span>")
         } else {
            append(i.contentToString())
         }
      }
   }
}