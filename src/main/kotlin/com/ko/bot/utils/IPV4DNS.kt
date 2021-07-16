package com.ko.bot.utils

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

// 210.140.131.223
// 210.140.131.219
// 210.140.131.201
// 210.140.131.199

class IPV4DNS: Dns {
    override fun lookup(hostname: String): MutableList<InetAddress> {
        if (hostname.isEmpty()) {
            return Dns.SYSTEM.lookup(hostname)
        } else {
            return try {
                val inetAddressList = mutableListOf<InetAddress>()

                val inetAddresses = InetAddress.getAllByName(hostname)

                for (inetAddress in inetAddresses) {
                    if (inetAddress is Inet4Address) {
                        inetAddressList.add(0, inetAddress)
                    } else {
                        inetAddressList.add(inetAddress)
                    }
                }
                inetAddressList
            } catch (ex: NullPointerException) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }
}